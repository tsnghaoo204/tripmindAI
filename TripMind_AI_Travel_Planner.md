# TripMind — AI Travel Planning Platform

## 1. Tổng quan đề tài

**Tên đề tài:** AI Travel Planner – Intelligent Travel Planning & Trip Management Platform

**Tên tiếng Việt:** Xây dựng nền tảng lập kế hoạch và quản lý chuyến du lịch thông minh sử dụng AI Agent và các dịch vụ API bên thứ ba.

TripMind là một nền tảng web hỗ trợ người dùng lập kế hoạch, tối ưu và quản lý chuyến du lịch. Hệ thống kết hợp dữ liệu nội bộ của người dùng, dữ liệu từ các API bên thứ ba và mô hình ngôn ngữ lớn (LLM).

Điểm khác biệt của hệ thống là AI không chỉ đóng vai trò chatbot. AI được triển khai theo kiến trúc **AI Agent + Tool Calling**, cho phép AI lựa chọn và gọi các công cụ nghiệp vụ, truy vấn dữ liệu của chuyến đi, sử dụng dữ liệu thời tiết/bản đồ/tỷ giá và đề xuất hoặc thực hiện thay đổi trên itinerary sau khi người dùng xác nhận.

---

# 2. Mục tiêu hệ thống

## 2.1. Mục tiêu chính

- Tạo và quản lý các chuyến du lịch.
- Tự động xây dựng lịch trình dựa trên nhu cầu người dùng.
- Cá nhân hóa lịch trình theo sở thích.
- Theo dõi và quản lý ngân sách.
- Tích hợp dữ liệu thời tiết thực tế.
- Tìm kiếm và quản lý địa điểm.
- Tối ưu thứ tự các địa điểm nhằm giảm thời gian di chuyển.
- Cho phép AI phân tích và thay đổi lịch trình.
- Tích hợp API bên thứ ba thông qua Tool Calling.
- Cho phép AI thực hiện nghiệp vụ có kiểm soát.
- Có cơ chế người dùng xác nhận trước khi thay đổi dữ liệu quan trọng.
- Lưu lịch sử hội thoại và lịch sử AI Tool Execution.

---

# 3. Đối tượng sử dụng

## 3.1. User

Người dùng thông thường có thể:

- Đăng ký / đăng nhập.
- Quản lý profile.
- Tạo chuyến đi.
- Thiết lập ngân sách.
- Thiết lập sở thích du lịch.
- Xem và chỉnh sửa itinerary.
- Tìm kiếm địa điểm.
- Lưu địa điểm yêu thích.
- Theo dõi chi phí.
- Chat với AI.
- Yêu cầu AI tối ưu lịch trình.
- Yêu cầu AI phân tích ngân sách.
- Yêu cầu AI điều chỉnh lịch trình theo thời tiết.

## 3.2. Admin

Admin có thể:

- Quản lý user.
- Quản lý destination.
- Quản lý place.
- Xem các chuyến đi.
- Theo dõi AI usage.
- Theo dõi tool execution.
- Theo dõi API usage.
- Xem system logs.

---

# 4. Core Features

## 4.1. Authentication

- Register
- Login
- Logout
- JWT authentication
- Refresh token nếu cần
- Role-based authorization

Roles:

```text
USER
ADMIN
```

---

# 5. Dashboard

Dashboard hiển thị:

- Upcoming trips.
- Recent trips.
- Saved places.
- Budget alerts.
- Weather alerts.
- AI suggestions.
- Progress của từng trip.

Ví dụ:

```text
Good morning!

Your upcoming trips

┌──────────────────────────────┐
│ Da Nang                      │
│ Oct 20 - Oct 23              │
│ 4 days · 2 travelers         │
│ 72% planned                  │
└──────────────────────────────┘

┌──────────────────────────────┐
│ Tokyo                        │
│ Nov 02 - Nov 07              │
│ 6 days · 2 travelers         │
│ 20% planned                  │
└──────────────────────────────┘
```

---

# 6. Create Trip

Quá trình tạo trip nên sử dụng wizard nhiều bước thay vì một form quá dài.

## Step 1 — Destination

```text
Where are you going?

Search destination
```

## Step 2 — Date

```text
Start date
End date
```

## Step 3 — Travelers

```text
Number of travelers
Travel type:
- Solo
- Couple
- Family
- Friends
```

## Step 4 — Budget

```text
Budget
Currency
```

## Step 5 — Preferences

Các preference có thể gồm:

```text
Food
Beach
Nature
Shopping
Photography
Nightlife
History
Culture
Adventure
Relaxation
```

## Step 6 — Travel style

```text
Relaxed
Balanced
Packed
```

Cuối cùng:

```text
Generate my trip
```

---

# 7. Trip Workspace

Mỗi trip có một workspace riêng.

Các tab:

```text
Overview
Itinerary
Map
Budget
Places
AI Assistant
```

Thông tin tổng quan:

```text
Da Nang
Oct 20 — Oct 23

4 days · 2 travelers

Budget:
8,000,000 VND

Weather:
29°C
```

---

# 8. Itinerary

Itinerary được chia theo từng ngày.

Ví dụ:

```text
Day 1 — October 20

09:00
Airport arrival

11:00
Lunch

14:00
Marble Mountains

17:30
My Khe Beach

19:30
Dinner
```

Mỗi activity có:

- Time.
- Place.
- Duration.
- Estimated cost.
- Transportation.
- Distance.
- Travel time.
- Notes.
- Order.

---

# 9. Map

Map hiển thị toàn bộ itinerary của từng ngày.

Ví dụ:

```text
Airport
   ↓
Marble Mountains
   ↓
Restaurant
   ↓
My Khe Beach
```

Có thể hiển thị:

- Marker.
- Route.
- Distance.
- Estimated travel time.
- Thứ tự hoạt động.

---

# 10. Budget Management

Người dùng nhập ngân sách tổng.

Ví dụ:

```text
Budget: 8,000,000 VND

Accommodation     2,000,000
Food              1,500,000
Transportation    1,000,000
Activities          850,000
Shopping            500,000
Other               300,000

Estimated:
7,350,000 VND

Remaining:
650,000 VND
```

Các chức năng:

- Budget overview.
- Expense tracking.
- Expense categories.
- Estimated cost.
- Actual cost.
- Remaining budget.
- Budget warning.
- AI budget analysis.

---

# 11. Places

Place có thể được lấy từ external provider.

Thông tin:

```text
Name
Category
Address
Latitude
Longitude
Rating
Price level
Opening hours
External ID
Provider
Metadata
```

Người dùng có thể:

- Search.
- View detail.
- Save.
- Add vào itinerary.

---

# 12. Saved Places

Người dùng có thể lưu các địa điểm yêu thích.

```text
User
  |
  └── Saved Places
          ├── Restaurant A
          ├── Beach B
          └── Museum C
```

Dữ liệu này có thể được AI sử dụng để cá nhân hóa recommendation.

---

# 13. AI Travel Assistant

AI Assistant được thiết kế như một sidebar trong Trip Workspace.

Ví dụ:

```text
┌──────────────────────────────────────┐
│ TripMind AI                     ×    │
├──────────────────────────────────────┤
│                                      │
│ You:                                 │
│ Đổi lịch ngày mai giúp tôi.          │
│                                      │
│ AI:                                  │
│ Tôi sẽ kiểm tra thời tiết và tìm     │
│ các hoạt động phù hợp.               │
│                                      │
│ [Review changes]                     │
│                                      │
├──────────────────────────────────────┤
│ Ask anything about your trip...      │
└──────────────────────────────────────┘
```

AI có thể hiểu context của trip hiện tại.

---

# 14. AI Use Cases

## 14.1. Generate itinerary

User:

```text
Tôi muốn đi Đà Nẵng 4 ngày, 2 người,
ngân sách 8 triệu, thích ăn uống,
biển và chụp ảnh.
```

AI có thể:

```text
get_destination()
get_weather()
search_places()
calculate_distance()
calculate_budget()
```

Sau đó tạo itinerary.

---

## 14.2. Weather-aware planning

User:

```text
Ngày mai trời mưa thì đổi lịch giúp tôi.
```

AI:

```text
get_current_trip()
        ↓
get_weather()
        ↓
search_places(indoor)
        ↓
calculate_distance()
        ↓
propose_changes()
```

AI đưa ra phương án thay đổi.

Người dùng:

```text
[Apply changes]
```

Sau đó backend mới cập nhật database.

---

## 14.3. Budget analysis

User:

```text
Tôi còn bao nhiêu tiền?
```

AI gọi:

```text
get_trip()
calculate_trip_budget()
```

Sau đó trả:

```text
Budget:
8,000,000 VND

Estimated spending:
7,350,000 VND

Remaining:
650,000 VND
```

---

## 14.4. Itinerary optimization

User:

```text
Tối ưu lịch ngày 2 để giảm thời gian di chuyển.
```

AI:

```text
get_itinerary()
        ↓
calculate_distance()
        ↓
calculate_travel_time()
        ↓
reorder_activities()
        ↓
propose_changes()
```

---

## 14.5. Place recommendation

User:

```text
Tìm cho tôi quán ăn gần khách sạn,
giá vừa phải và rating cao.
```

AI gọi:

```text
search_places()
calculate_distance()
```

Sau đó trả recommendation.

---

# 15. AI Agent Architecture

AI Agent gồm:

```text
User
  ↓
React
  ↓
Spring Boot
  ↓
AI Controller
  ↓
Agent Orchestrator
  ↓
LLM
  ↓
Tool Calling
```

Tool có thể gọi:

```text
Internal Tools
├── get_current_trip
├── get_itinerary
├── get_user_preferences
├── add_activity
├── remove_activity
├── update_activity
├── calculate_trip_budget
└── get_saved_places

External Tools
├── get_weather
├── search_places
├── geocode_location
├── get_exchange_rate
└── calculate_distance
```

---

# 16. Agent Loop

Agent có thể hoạt động theo loop:

```text
User Request
     ↓
    LLM
     ↓
Need Tool?
  ┌──┴──┐
 Yes    No
  ↓      ↓
Tool    Final
Call    Answer
  ↓
Tool Result
  ↓
LLM
  ↓
Need another tool?
```

Pseudo-code:

```java
while (!finalAnswer) {

    response = llm.chat(messages, tools);

    if (response.hasToolCall()) {

        ToolResult result =
            toolExecutor.execute(
                response.getToolCall()
            );

        messages.add(result);

    } else {

        return response.getAnswer();
    }
}
```

---

# 17. Tool Calling

LLM không truy cập database trực tiếp.

Không sử dụng:

```text
LLM → PostgreSQL
```

Mà sử dụng:

```text
LLM
 ↓
Tool
 ↓
Service
 ↓
Repository
 ↓
PostgreSQL
```

Ví dụ:

```text
LLM
 ↓
get_current_trip()
 ↓
TripService
 ↓
TripRepository
 ↓
PostgreSQL
```

Backend chịu trách nhiệm:

- Authentication.
- Authorization.
- Validation.
- Business rules.
- Rate limiting.
- Logging.
- Transaction.
- Data access.

---

# 18. Human Approval

Các thao tác làm thay đổi dữ liệu quan trọng nên có cơ chế:

```text
AI proposes action
        ↓
User reviews
        ↓
User approves
        ↓
Backend executes
        ↓
Database update
```

Ví dụ:

```text
AI Proposed Changes

REMOVE
My Khe Beach — 15:00

ADD
Museum of Cham Sculpture — 14:30
Han Market — 16:30

Estimated cost:
+50,000 VND

[Apply changes]
[Cancel]
```

---

# 19. AI Tool Execution Log

Hệ thống lưu lại mỗi lần AI gọi tool.

Ví dụ:

```text
AI Activity

✓ get_current_trip
  120 ms

✓ get_weather
  320 ms

✓ search_places
  430 ms

✓ calculate_distance
  85 ms

✓ calculate_trip_budget
  12 ms

✓ update_itinerary
  240 ms
```

Mục đích:

- Debug.
- Audit.
- Theo dõi AI.
- Thống kê API.
- Phục vụ Admin Dashboard.
- Chứng minh Agent thực sự sử dụng Tool Calling.

---

# 20. Database Design

## 20.1. users

```text
users
- id
- email
- password_hash
- name
- avatar_url
- role
- created_at
- updated_at
```

## 20.2. user_preferences

```text
user_preferences
- id
- user_id
- travel_style
- budget_preference
- preferences_json
- created_at
- updated_at
```

## 20.3. destinations

```text
destinations
- id
- name
- country
- latitude
- longitude
- timezone
- metadata
- created_at
```

## 20.4. trips

```text
trips
- id
- user_id
- destination_id
- name
- start_date
- end_date
- travelers
- budget
- currency
- status
- created_at
- updated_at
```

## 20.5. itinerary_days

```text
itinerary_days
- id
- trip_id
- date
- day_number
```

## 20.6. activities

```text
activities
- id
- itinerary_day_id
- place_id
- start_time
- end_time
- estimated_cost
- transportation_mode
- notes
- order_index
```

## 20.7. places

```text
places
- id
- external_id
- provider
- name
- category
- latitude
- longitude
- rating
- price_level
- address
- opening_hours
- metadata
```

## 20.8. saved_places

```text
saved_places
- id
- user_id
- place_id
- created_at
```

## 20.9. expenses

```text
expenses
- id
- trip_id
- category
- amount
- currency
- description
- expense_date
- created_at
```

## 20.10. conversations

```text
conversations
- id
- user_id
- trip_id
- created_at
- updated_at
```

## 20.11. messages

```text
messages
- id
- conversation_id
- role
- content
- created_at
```

## 20.12. ai_tool_executions

```text
ai_tool_executions
- id
- conversation_id
- tool_name
- arguments
- result
- status
- execution_time
- created_at
```

## 20.13. weather_cache

```text
weather_cache
- id
- latitude
- longitude
- forecast_date
- temperature
- precipitation_probability
- weather_code
- fetched_at
```

---

# 21. Database Relationships

```text
User
 │
 ├── UserPreference
 │
 ├── Trip
 │    │
 │    ├── Destination
 │    │
 │    ├── ItineraryDay
 │    │       └── Activity
 │    │              └── Place
 │    │
 │    ├── Expense
 │    │
 │    └── Conversation
 │             └── Message
 │
 └── SavedPlace
          └── Place
```

---

# 22. Recommended ERD

```text
users
  │
  ├────────────── user_preferences
  │
  ├────────────── trips
  │                    │
  │                    ├──────── destinations
  │                    │
  │                    ├──────── itinerary_days
  │                    │               │
  │                    │               └──── activities
  │                    │                         │
  │                    │                         └──── places
  │                    │
  │                    ├──────── expenses
  │                    │
  │                    └──────── conversations
  │                                    │
  │                                    └──── messages
  │
  └────────────── saved_places
                       │
                       └──── places
```

---

# 23. REST API

## Authentication

```http
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/logout
```

## Trips

```http
GET    /api/trips
POST   /api/trips
GET    /api/trips/{id}
PUT    /api/trips/{id}
DELETE /api/trips/{id}
```

## Itinerary

```http
GET    /api/trips/{id}/itinerary
POST   /api/trips/{id}/itinerary/activities
PUT    /api/activities/{id}
DELETE /api/activities/{id}
```

## Budget

```http
GET  /api/trips/{id}/budget
POST /api/trips/{id}/expenses
GET  /api/trips/{id}/expenses
```

## Places

```http
GET  /api/places
GET  /api/places/{id}
POST /api/places/{id}/save
DELETE /api/places/{id}/save
```

## AI

```http
POST /api/trips/{id}/ai/chat
POST /api/trips/{id}/ai/apply
GET  /api/trips/{id}/ai/activity
```

---

# 24. UI/UX Structure

## Public

```text
Landing Page
Explore
Destination Detail
Login
Register
```

## Authenticated

```text
Dashboard
My Trips
Trip Workspace
  ├── Overview
  ├── Itinerary
  ├── Map
  ├── Budget
  ├── Places
  └── AI Assistant

Saved Places
Profile
Settings
```

## Admin

```text
Admin Dashboard
Users
Trips
Destinations
Places
AI Usage
Tool Executions
API Usage
System Logs
```

---

# 25. Frontend Tech Stack

```text
React
TypeScript
Vite
Tailwind CSS
React Router
TanStack Query
Zustand
Leaflet
```

Recommended responsibilities:

- React: UI.
- TypeScript: type safety.
- Vite: development/build.
- Tailwind: styling.
- React Router: routing.
- TanStack Query: server state.
- Zustand: local/global UI state.
- Leaflet: map visualization.

---

# 26. Backend Tech Stack

```text
Java 17
Spring Boot
Spring Web
Spring Security
JWT
Spring Data JPA
Hibernate
PostgreSQL
Redis
```

Backend architecture:

```text
controller/
service/
repository/
entity/
dto/
mapper/
security/
exception/
integration/
ai/
config/
```

---

# 27. AI Architecture

```text
ai/
├── AgentOrchestrator
├── ToolRegistry
├── ToolExecutor
├── PromptService
├── ConversationService
└── tools/
    ├── WeatherTool
    ├── PlaceSearchTool
    ├── GeocodingTool
    ├── CurrencyTool
    ├── DistanceTool
    ├── TripTool
    ├── ItineraryTool
    └── BudgetTool
```

---

# 28. External API Integration

## Weather

Recommended:

```text
Open-Meteo
```

Use cases:

- Current weather.
- Forecast.
- Historical weather.
- Precipitation probability.
- Temperature.
- Weather-aware itinerary.

## Currency

Recommended:

```text
Frankfurter
```

Use cases:

- Exchange rate.
- Currency conversion.
- Historical exchange rate.

## Maps / Geocoding

Possible:

```text
OpenStreetMap
Nominatim
Leaflet
Mapbox
Google Maps / Places
```

Nên tạo abstraction:

```java
interface PlaceSearchProvider {
    List<Place> search(PlaceSearchRequest request);
}
```

AI chỉ gọi:

```text
search_places()
```

Provider cụ thể được backend xử lý.

---

# 29. External Integration Architecture

```text
AI Agent
    │
    ├── WeatherTool
    │       └── OpenMeteoClient
    │
    ├── CurrencyTool
    │       └── FrankfurterClient
    │
    ├── PlaceSearchTool
    │       └── PlaceSearchProvider
    │
    └── GeocodingTool
            └── GeocodingProvider
```

Không để LLM trực tiếp gọi API.

---

# 30. Redis

Redis dùng cho:

- Weather cache.
- Currency cache.
- Place search cache.
- Rate limiting.
- Temporary AI state nếu cần.

Ví dụ:

```text
weather:{latitude}:{longitude}:{date}
currency:{from}:{to}
places:{query_hash}
```

---

# 31. Security

Backend cần kiểm soát:

- JWT authentication.
- Role-based authorization.
- User ownership của trip.
- Tool permissions.
- Input validation.
- API rate limiting.
- AI action approval.
- Audit log.
- External API key protection.

AI không được phép tự ý:

```text
DELETE user
DELETE trip
CHANGE critical data
```

mà không có permission/approval phù hợp.

---

# 32. Backend Project Structure

```text
src/main/java/com/tripmind

├── auth
│   ├── controller
│   ├── service
│   ├── dto
│   └── security
│
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   └── entity
│
├── trip
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── itinerary
│   ├── controller
│   ├── service
│   ├── repository
│   └── entity
│
├── place
│   ├── controller
│   ├── service
│   ├── repository
│   └── entity
│
├── budget
│   ├── controller
│   ├── service
│   └── entity
│
├── ai
│   ├── AgentOrchestrator
│   ├── ToolRegistry
│   ├── ToolExecutor
│   ├── ConversationService
│   └── tools
│
├── integration
│   ├── weather
│   ├── currency
│   └── maps
│
└── common
    ├── exception
    ├── response
    └── config
```

---

# 33. Example AI Flow

User:

```text
Ngày mai trời mưa, tối ưu lịch trình
cho tôi và giữ ngân sách dưới 8 triệu.
```

Agent:

```text
1. get_current_trip()
2. get_itinerary()
3. get_weather()
4. search_places(indoor)
5. calculate_distance()
6. calculate_trip_budget()
7. generate_proposed_changes()
```

Response:

```text
Tôi phát hiện ngày mai có khả năng mưa cao.

Tôi đề xuất:

REMOVE
My Khe Beach

ADD
Museum of Cham Sculpture
Han Market

Travel time:
-18 minutes

Estimated cost:
+50,000 VND

Budget:
7,400,000 / 8,000,000 VND

[Apply changes]
```

User bấm:

```text
Apply changes
```

Backend:

```text
POST /api/trips/{id}/ai/apply
        ↓
validate action
        ↓
transaction
        ↓
update itinerary
        ↓
PostgreSQL
```

---

# 34. Core Differentiator

Sản phẩm không phải:

```text
Chatbot + Travel Website
```

Mà là:

```text
AI Agentic Travel Platform
```

AI có thể:

```text
Understand
   ↓
Retrieve
   ↓
Call tools
   ↓
Analyze
   ↓
Plan
   ↓
Propose action
   ↓
User approval
   ↓
Execute
   ↓
Persist state
```

Đây là điểm quan trọng nhất của đề tài.

---

# 35. Tại sao ChatGPT không thay thế được website?

ChatGPT có thể tạo một itinerary dạng text.

TripMind có:

```text
User data
+
Trip state
+
Itinerary database
+
Saved places
+
Budget
+
Expenses
+
Weather API
+
Map data
+
External services
+
Tool Calling
+
Business rules
+
User approval
```

Ví dụ:

```text
ChatGPT:
"Tôi đề xuất bạn đi bảo tàng."

TripMind:
"Ngày 2 của trip #123 hiện đang có
4 activities. Dự báo mưa 80%.

Tôi đã tìm 5 địa điểm indoor,
lọc theo sở thích của bạn,
tính khoảng cách,
kiểm tra ngân sách và đề xuất
thay thế 2 activities.

Bạn có muốn áp dụng?"
```

---

# 36. MVP

## Phase 1 — Core Website

```text
Authentication
Trip CRUD
Destination
Itinerary
Places
Budget
Expenses
```

## Phase 2 — External APIs

```text
Weather
Currency
Geocoding
Map
Place Search
```

## Phase 3 — AI

```text
LLM integration
Tool Calling
Agent Loop
Conversation
Conversation history
```

## Phase 4 — Intelligent Features

```text
AI itinerary generation
Weather-aware planning
Itinerary optimization
Budget analysis
Personalized recommendation
```

## Phase 5 — Production Features

```text
Redis
Caching
Tool execution logs
Rate limiting
RBAC
Docker
Swagger
Testing
Deployment
```

---

# 37. Những thứ không nên làm trong MVP

Không nên mở rộng quá mức sang:

```text
Flight booking
Hotel booking
Payment
Social network
Real-time chat giữa users
Mobile app
Complex travel marketplace
```

Các chức năng này có thể để ở Future Work.

---

# 38. Testing

## Backend

```text
JUnit
Mockito
Spring Boot Test
Testcontainers
```

Test:

- Authentication.
- Trip service.
- Itinerary service.
- Budget calculation.
- Tool execution.
- Permission.
- AI action approval.

## API

```text
Postman
Swagger / OpenAPI
```

## Frontend

Có thể bổ sung:

```text
Vitest
React Testing Library
```

---

# 39. Deployment

```text
                    Internet
                       │
                       ▼
                  Nginx
                  /      \
                 /        \
                ▼          ▼
           React App   Spring Boot
                            │
                    ┌───────┴───────┐
                    ▼               ▼
               PostgreSQL         Redis
                    │
                    ▼
              External APIs
```

Docker Compose:

```text
services:
  frontend
  backend
  postgres
  redis
  nginx
```

---

# 40. Recommended Final Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React + TypeScript |
| Build | Vite |
| UI | Tailwind CSS |
| State | Zustand |
| Server State | TanStack Query |
| Map | Leaflet |
| Backend | Java 17 + Spring Boot |
| API | REST |
| Security | Spring Security + JWT |
| ORM | Spring Data JPA / Hibernate |
| Database | PostgreSQL |
| Cache | Redis |
| AI | LLM API |
| AI Architecture | AI Agent + Tool Calling |
| Weather | Open-Meteo |
| Currency | Frankfurter |
| Geocoding | Nominatim / Provider abstraction |
| Places | OSM / Mapbox / Google Places |
| API Documentation | Swagger / OpenAPI |
| Testing | JUnit + Mockito + Testcontainers |
| Container | Docker |
| Deployment | VPS / AWS |
| Reverse Proxy | Nginx |

---

# 41. Demo Scenario

Scenario đề xuất để bảo vệ đồ án:

### Step 1

User tạo:

```text
Da Nang
4 days
2 travelers
8,000,000 VND
Food + Beach + Photography
Relaxed
```

### Step 2

AI tạo itinerary.

### Step 3

Website hiển thị:

- Timeline.
- Map.
- Estimated budget.
- Weather.

### Step 4

User hỏi:

```text
Ngày mai nếu trời mưa thì sao?
```

### Step 5

AI gọi:

```text
get_weather()
search_places()
calculate_distance()
calculate_budget()
```

### Step 6

AI đưa ra proposed changes.

### Step 7

User:

```text
Apply changes
```

### Step 8

Database được cập nhật.

### Step 9

User hỏi:

```text
Tôi đang vượt ngân sách bao nhiêu?
```

AI đọc dữ liệu trip và trả kết quả.

### Step 10

Admin mở AI Activity:

```text
get_weather
search_places
calculate_distance
calculate_budget
update_itinerary
```

Qua đó chứng minh toàn bộ pipeline:

```text
LLM
→ Tool Calling
→ Internal/External APIs
→ Business Logic
→ User Approval
→ Database
```

---

# 42. Các điểm có thể trình bày trong báo cáo

## Chương 1 — Tổng quan

- Lý do chọn đề tài.
- Bài toán.
- Mục tiêu.
- Phạm vi.
- Đối tượng sử dụng.

## Chương 2 — Công nghệ

- React.
- Spring Boot.
- PostgreSQL.
- Redis.
- REST API.
- JWT.
- LLM.
- Tool Calling.
- AI Agent.

## Chương 3 — Phân tích hệ thống

- Functional requirements.
- Non-functional requirements.
- Use Case.
- Activity Diagram.
- Sequence Diagram.

## Chương 4 — Thiết kế

- Architecture.
- Database.
- ERD.
- API.
- AI Agent architecture.
- Tool architecture.
- Security.

## Chương 5 — Triển khai

- Frontend.
- Backend.
- Database.
- External APIs.
- AI integration.
- Tool Calling.

## Chương 6 — Kiểm thử

- Unit Test.
- Integration Test.
- API Test.
- AI Tool Test.
- Security Test.

## Chương 7 — Kết quả

- UI.
- AI Agent.
- Itinerary generation.
- Weather adaptation.
- Budget analysis.
- Tool execution.

## Chương 8 — Hạn chế và hướng phát triển

- Booking.
- Flight API.
- Hotel API.
- Recommendation model.
- Vector database.
- RAG.
- Multi-agent.
- Mobile application.
