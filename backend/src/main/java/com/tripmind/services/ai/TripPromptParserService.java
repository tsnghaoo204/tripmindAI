package com.tripmind.services.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.entities.DestinationEntity;
import com.tripmind.repositories.DestinationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripPromptParserService {

    private final LlmGateway llm;
    private final DestinationRepository destinationRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> parsePrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return Collections.emptyMap();
        }

        Map<String, Object> result = new LinkedHashMap<>();

        // 1. Phân tích ngữ nghĩa động bằng Gemini nếu đã cấu hình
        if (llm.isConfigured()) {
            try {
                String systemPrompt = """
                    Bạn là trợ lý AI chuyên phân tích yêu cầu lập kế hoạch du lịch bằng tiếng Việt hoặc tiếng Anh.
                    Hãy đọc kỹ câu mô tả của người dùng và trích xuất thành một JSON có các trường sau:
                    - destination: tên địa điểm/thành phố/quốc gia người dùng muốn đến (VD: "Nha Trang", "Đà Lạt", "Tokyo", "Côn Đảo", "Paris"...)
                    - country: quốc gia tương ứng (VD: "Việt Nam", "Nhật Bản", "Pháp"...)
                    - latitude: toạ độ vĩ độ xấp xỉ của địa danh (số thực, VD: 12.2388)
                    - longitude: toạ độ kinh độ xấp xỉ của địa danh (số thực, VD: 109.1967)
                    - timezone: múi giờ chuẩn IANA (chuỗi, VD: "Asia/Ho_Chi_Minh", "Asia/Tokyo", "Europe/Paris"...)
                    - days: số ngày đi (số nguyên, VD: "2 ngày 1 đêm" -> 2; nếu không nhắc thì mặc định 3)
                    - nights: số đêm (số nguyên, VD: "2 ngày 1 đêm" -> 1; nếu không nhắc thì mặc định 2)
                    - travelers: số lượng người đi (số nguyên, VD: "cho 2 người", "cùng người yêu" -> 2; mặc định 2)
                    - budget: dự toán ngân sách bằng VND (số nguyên, VD: "4 triệu" -> 4000000, "5tr" -> 5000000; nếu không có thì null)
                    - travelStyle: "RELAXED" (nếu có từ chill, thư thả, nghỉ dưỡng), "FAST_PACED" (nếu có từ trải nghiệm, khám phá nhiều), hoặc "BALANCED" (mặc định)
                    - childrenCount: số trẻ em (dưới 12 tuổi, mặc định 0)
                    - seniorsCount: số người cao tuổi (> 60 tuổi, mặc định 0)
                    - hasVegetarian: boolean (true nếu có ăn chay)
                    - hasMobilityDifficulty: boolean (true nếu khó đi lại, hạn chế đi bộ xa)

                    Chỉ trả về DUY NHẤT một chuỗi JSON hợp lệ, không bọc markdown ```json và không có văn bản giải thích.
                    """;

                Prompt prompt = new Prompt(
                        List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                        OpenAiChatOptions.builder().temperature(0.1).build()
                );

                String text = llm.call(prompt).getResult().getOutput().getText();
                if (text != null && text.contains("{") && text.contains("}")) {
                    int start = text.indexOf('{');
                    int end = text.lastIndexOf('}');
                    JsonNode node = objectMapper.readTree(text.substring(start, end + 1));

                    String destination = node.has("destination") ? node.get("destination").asText() : "";
                    String country = node.has("country") ? node.get("country").asText() : "Việt Nam";
                    Double lat = node.has("latitude") && !node.get("latitude").isNull() ? node.get("latitude").asDouble() : 16.0544;
                    Double lng = node.has("longitude") && !node.get("longitude").isNull() ? node.get("longitude").asDouble() : 108.2022;
                    String timezone = node.has("timezone") ? node.get("timezone").asText() : "Asia/Ho_Chi_Minh";

                    int days = node.has("days") && node.get("days").asInt() > 0 ? node.get("days").asInt() : 3;
                    int nights = node.has("nights") && node.get("nights").asInt() >= 0 ? node.get("nights").asInt() : Math.max(1, days - 1);
                    int travelers = node.has("travelers") && node.get("travelers").asInt() > 0 ? node.get("travelers").asInt() : 2;
                    Long budget = node.has("budget") && !node.get("budget").isNull() ? node.get("budget").asLong() : null;
                    String travelStyle = node.has("travelStyle") ? node.get("travelStyle").asText() : "BALANCED";
                    int childrenCount = node.has("childrenCount") ? node.get("childrenCount").asInt() : 0;
                    int seniorsCount = node.has("seniorsCount") ? node.get("seniorsCount").asInt() : 0;
                    boolean hasVegetarian = node.has("hasVegetarian") && node.get("hasVegetarian").asBoolean();
                    boolean hasMobilityDifficulty = node.has("hasMobilityDifficulty") && node.get("hasMobilityDifficulty").asBoolean();

                    LocalDate tomorrow = LocalDate.now().plusDays(1);
                    LocalDate returnDate = tomorrow.plusDays(days - 1);

                    result.put("title", "Kế hoạch du lịch " + destination + " " + days + " ngày " + nights + " đêm");
                    result.put("destinationName", destination);
                    result.put("country", country);
                    result.put("latitude", lat);
                    result.put("longitude", lng);
                    result.put("timezone", timezone);
                    result.put("startDate", tomorrow.toString());
                    result.put("endDate", returnDate.toString());
                    result.put("days", days);
                    result.put("nights", nights);
                    result.put("travelers", travelers);
                    result.put("budget", budget != null ? budget : (long) (days * travelers * 800000));
                    result.put("travelStyle", travelStyle);
                    result.put("childrenCount", childrenCount);
                    result.put("seniorsCount", seniorsCount);
                    result.put("hasVegetarian", hasVegetarian);
                    result.put("hasMobilityDifficulty", hasMobilityDifficulty);

                    // Tra cứu động trong CSDL (không gán cứng)
                    if (!destination.isBlank()) {
                        List<DestinationEntity> matches = destinationRepository.findByNameContainingIgnoreCase(destination.trim());
                        if (!matches.isEmpty()) {
                            result.put("destinationId", matches.get(0).getId());
                            result.put("isCustomDestination", false);
                        } else {
                            result.put("destinationId", null);
                            result.put("isCustomDestination", true);
                        }
                    }

                    return result;
                }
            } catch (Exception e) {
                log.warn("Gemini parse prompt that bai, dung bo phan tich dong: {}", e.getMessage());
            }
        }

        // 2. Dự phòng động không fix cứng bất kỳ thành phố nào
        return dynamicFallbackParse(userPrompt);
    }

    private Map<String, Object> dynamicFallbackParse(String prompt) {
        Map<String, Object> res = new LinkedHashMap<>();
        String p = prompt.trim();

        // Bóc tách tên địa điểm theo cấu trúc câu tiếng Việt ("đi <tên>", "đến <tên>", "du lịch <tên>")
        String dest = "";
        java.util.regex.Matcher mDest = java.util.regex.Pattern
                .compile("(?:đi|đến|tới|du lịch)\\s+([a-zA-ZÀ-ỹ\\s]+?)(?:\\s+\\d+\\s*(?:ngày|n|đêm)|\\s+trong|\\s+cho|\\s+cùng|\\s+ngân|\\s*$)",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(p);
        if (mDest.find()) {
            dest = mDest.group(1).trim();
        }
        if (dest.isBlank()) {
            dest = "Điểm đến tùy chọn";
        }

        int days = 3;
        int nights = 2;
        java.util.regex.Matcher mDays = java.util.regex.Pattern
                .compile("(\\d+)\\s*ngày\\s*(\\d+)\\s*đêm|(\\d+)\\s*n\\s*(\\d+)\\s*đ", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(p);
        if (mDays.find()) {
            String d = mDays.group(1) != null ? mDays.group(1) : mDays.group(3);
            String n = mDays.group(2) != null ? mDays.group(2) : mDays.group(4);
            days = Integer.parseInt(d);
            nights = Integer.parseInt(n);
        } else {
            java.util.regex.Matcher mDaysOnly = java.util.regex.Pattern
                    .compile("(\\d+)\\s*ngày", java.util.regex.Pattern.CASE_INSENSITIVE)
                    .matcher(p);
            if (mDaysOnly.find()) {
                days = Integer.parseInt(mDaysOnly.group(1));
                nights = Math.max(1, days - 1);
            }
        }

        int travelers = 2;
        java.util.regex.Matcher mTravelers = java.util.regex.Pattern
                .compile("(?:cho|cùng|\\+)?\\s*(\\d+)\\s*(?:người|bạn|khách)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(p);
        if (mTravelers.find()) {
            travelers = Integer.parseInt(mTravelers.group(1));
        } else if (p.toLowerCase().contains("người yêu") || p.toLowerCase().contains("cặp đôi") || p.toLowerCase().contains("vợ")) {
            travelers = 2;
        } else if (p.toLowerCase().contains("một mình") || p.toLowerCase().contains("solo") || p.toLowerCase().contains("1 người")) {
            travelers = 1;
        } else if (p.toLowerCase().contains("gia đình")) {
            travelers = 4;
        }

        long budget = 5000000L;
        java.util.regex.Matcher mTrieu = java.util.regex.Pattern
                .compile("(\\d+(?:[.,]\\d+)?)\\s*(?:triệu|tr|củ)", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(p);
        if (mTrieu.find()) {
            budget = (long) (Double.parseDouble(mTrieu.group(1).replace(",", ".")) * 1000000);
        } else {
            budget = Math.max(3000000L, (long) days * travelers * 800000L);
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate returnDate = tomorrow.plusDays(days - 1);

        res.put("title", "Kế hoạch du lịch " + dest + " " + days + " ngày " + nights + " đêm");
        res.put("destinationName", dest);
        res.put("country", "Việt Nam");
        res.put("startDate", tomorrow.toString());
        res.put("endDate", returnDate.toString());
        res.put("days", days);
        res.put("nights", nights);
        res.put("travelers", travelers);
        res.put("budget", budget);
        res.put("travelStyle", p.toLowerCase().contains("chill") || p.toLowerCase().contains("thư thả") ? "RELAXED" : "BALANCED");
        res.put("childrenCount", p.toLowerCase().contains("bé") || p.toLowerCase().contains("trẻ") ? 1 : 0);
        res.put("seniorsCount", p.toLowerCase().contains("già") || p.toLowerCase().contains("ông bà") ? 1 : 0);
        res.put("hasVegetarian", p.toLowerCase().contains("chay"));
        res.put("hasMobilityDifficulty", p.toLowerCase().contains("đi lại"));

        List<DestinationEntity> matches = destinationRepository.findByNameContainingIgnoreCase(dest.trim());
        if (!matches.isEmpty()) {
            res.put("destinationId", matches.get(0).getId());
            res.put("isCustomDestination", false);
        } else {
            res.put("destinationId", null);
            res.put("isCustomDestination", true);
        }

        return res;
    }
}
