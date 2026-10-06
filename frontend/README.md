# TripMind AI — Frontend Architecture

Cấu trúc thư mục frontend được chuẩn hoá từ mô hình kiến trúc **RoadAI FE ver2** (Vite + React 19 + TypeScript + Tailwind CSS v4 + TanStack Router & Query + Radix/shadcn UI Primitives) và điều chỉnh tối ưu cho bài toán **TripMind AI Travel Planner**.

---

## 📁 Cấu trúc thư mục

```
frontend/
├── public/                     # Static assets (favicon, images, icons)
├── src/
│   ├── client/                 # Tầng giao tiếp API Backend (Spring Boot)
│   │   ├── api/                # API caller functions phân theo module (trip, itinerary, ai, budget...)
│   │   ├── types/              # TypeScript interfaces khớp với DTOs của Backend (ADS-30)
│   │   └── client.ts           # Axios client có JWT interceptor, refresh logic & auto logout
│   ├── components/             # Thành phần giao diện
│   │   ├── common/             # Component dùng chung (Header, Footer, Logo, NotFound, DataTable)
│   │   ├── pages/              # Component riêng biệt theo từng module/màn hình
│   │   │   ├── auth/           # Login, Register, Profile cards
│   │   │   ├── trips/          # TripCard, TripList, CreateTripModal, TripFilters
│   │   │   ├── itinerary/      # Timeline theo ngày, ActivityItem, MapView, ReorderList
│   │   │   ├── ai/             # Chat Assistant SSE, ProposalCard, UndoModal, ExplanationModal
│   │   │   ├── budget/         # BudgetSummaryCard, ExpenseTable, DailyAllowanceWidget
│   │   │   ├── checklist/      # ChecklistGroup, ItemRow, AI SuggestionDrawer
│   │   │   ├── places/         # PlaceCard, SearchInput, PlaceDetailModal, RatingStars
│   │   │   └── admin/          # ToolExecutionAuditTable, AiUsageMetricsChart
│   │   └── ui/                 # Atomic UI primitives chuẩn shadcn / Radix UI
│   │       ├── button.tsx, input.tsx, card.tsx, badge.tsx, sonner.tsx, dialog.tsx...
│   │
│   ├── config/                 # Cấu hình tĩnh (navigation, constants, app settings)
│   ├── contexts/               # React Contexts (ThemeContext, AuthContext)
│   ├── hooks/                  # Custom React hooks & TanStack Query mutations (useTrips, useAuth...)
│   ├── lib/                    # Utilities cơ bản (utils.ts: cn, formatCurrency, formatDateRange)
│   ├── routes/                 # File-based Routing (TanStack Router)
│   │   ├── __root.tsx          # Root route bọc Theme & Auth providers, Toaster
│   │   ├── _layout.tsx         # Layout chính (Header + Main content + Footer)
│   │   ├── _layout/
│   │   │   ├── index.tsx       # Danh sách chuyến đi & Dashboard
│   │   │   └── places.tsx      # Màn hình Khám phá & Lưu địa điểm
│   │   ├── login.tsx           # Trang đăng nhập
│   │   └── register.tsx        # Trang đăng ký
│   │
│   ├── utils/                  # Hàm tiện ích xử lý lỗi, validate, local storage
│   ├── index.css               # Tailwind CSS v4 & Thiết lập Design Tokens (Light/Dark mode)
│   ├── theme.postcss           # Theme engine định nghĩa biến CSS theo chuẩn Tailwind v4
│   ├── main.tsx                # Bootstrap ứng dụng (QueryClientProvider, RouterProvider)
│   ├── routeTree.gen.ts        # Route tree được TanStack Router plugin biên dịch tự động
│   └── vite-env.d.ts           # Định nghĩa biến môi trường Vite
│
├── .env.example                # Mẫu biến môi trường
├── biome.json                  # Cấu hình linter & formatter (thay thế ESLint/Prettier)
├── components.json             # Cấu hình shadcn UI CLI
├── index.html                  # HTML entry point với Plus Jakarta Sans font
├── package.json                # Dependencies & scripts
├── tsconfig.json               # Cấu hình TypeScript
└── vite.config.ts              # Cấu hình Vite (SWC, Tailwind v4, TanStack Router plugin, Backend Proxy)
```

---

## 🚀 Khởi chạy dự án

```bash
# 1. Cài đặt dependencies
npm install   # hoặc: bun install / pnpm install

# 2. Khởi chạy dev server (cổng mặc định 5173, tự động proxy /api sang backend 8088)
npm run dev
```
