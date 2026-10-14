package com.tripmind.configurations.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripmind.domains.responses.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Chặn thân request lớn hơn {@code tripmind.http.max-body-size} (mặc định 1MB) với 413: theo
 * Content-Length khi có, và đếm byte thực đọc khi gửi dạng chunked.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class RequestSizeLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Value("${tripmind.http.max-body-size:1MB}")
    private DataSize maxBodySize;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long limit = maxBodySize.toBytes();
        if (request.getContentLengthLong() > limit) {
            response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.fail("PAYLOAD_TOO_LARGE",
                    "Request body exceeds " + limit + " bytes", Map.of("maxBytes", limit)));
            return;
        }
        chain.doFilter(new LimitedRequest(request, limit), response);
    }

    /** Thân request vượt giới hạn khi đọc dần (chunked). */
    public static class PayloadTooLargeException extends IOException {
        public PayloadTooLargeException(long limit) {
            super("Request body exceeds " + limit + " bytes");
        }
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long limit;
        private ServletInputStream stream;

        LimitedRequest(HttpServletRequest request, long limit) {
            super(request);
            this.limit = limit;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (stream == null) {
                stream = new LimitedInputStream(super.getInputStream(), limit);
            }
            return stream;
        }

        @Override
        public BufferedReader getReader() throws IOException {
            String encoding = getCharacterEncoding();
            Charset charset = encoding != null ? Charset.forName(encoding) : StandardCharsets.UTF_8;
            return new BufferedReader(new InputStreamReader(getInputStream(), charset));
        }
    }

    private static final class LimitedInputStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long limit;
        private long read;

        LimitedInputStream(ServletInputStream delegate, long limit) {
            this.delegate = delegate;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int b = delegate.read();
            if (b >= 0) {
                count(1);
            }
            return b;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int n = delegate.read(buffer, offset, length);
            if (n > 0) {
                count(n);
            }
            return n;
        }

        private void count(int n) throws PayloadTooLargeException {
            read += n;
            if (read > limit) {
                throw new PayloadTooLargeException(limit);
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
