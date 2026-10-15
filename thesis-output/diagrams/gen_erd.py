"""ERD (bằng chứng: spec/data-model.md). Chỉ thể hiện các bảng có thực thể và được sử dụng."""
from common import SRC
from drawio_lib import Doc, erd_rel, erd_table


def erd_core():
    d = Doc("ERD nhóm chuyến đi")
    _, users = erd_table(d, "users", 20, 20, [
        ("PK", "id", "bigint"), ("UQ", "email", "varchar(255)"), ("", "password_hash", "varchar(72)"),
        ("", "name", "varchar(120)"), ("", "avatar_url", "text"), ("", "role", "varchar(16)"),
        ("", "is_active", "boolean"), ("", "token_version", "int")])
    _, pref = erd_table(d, "user_preferences", 20, 260, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("", "travel_style", "varchar(16)"),
        ("", "budget_preference", "varchar(16)"), ("", "preferences_json", "jsonb")])
    _, dest = erd_table(d, "destinations", 20, 430, [
        ("PK", "id", "bigint"), ("", "provider", "varchar(24)"), ("", "external_id", "varchar(255)"),
        ("", "name", "varchar(160)"), ("", "country", "varchar(80)"), ("", "latitude / longitude", "numeric"),
        ("", "timezone", "varchar(64)")])
    _, trips = erd_table(d, "trips", 330, 20, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "destination_id", "bigint"),
        ("", "name", "varchar(160)"), ("", "start_date / end_date", "date"), ("", "travelers", "smallint"),
        ("", "budget", "bigint"), ("", "currency", "char(3)"), ("", "travel_style", "varchar(16)"),
        ("", "group_profile", "jsonb"), ("", "travel_plan", "jsonb"), ("", "phase_override", "varchar(8)")])
    _, days = erd_table(d, "itinerary_days", 330, 340, [
        ("PK", "id", "bigint"), ("FK", "trip_id", "bigint"), ("", "day_number", "smallint"),
        ("", "date", "date"), ("", "note", "text")])
    _, acts = erd_table(d, "activities", 640, 260, [
        ("PK", "id", "bigint"), ("FK", "itinerary_day_id", "bigint"), ("FK", "place_id", "bigint"),
        ("", "title", "varchar(200)"), ("", "activity_type", "varchar(16)"), ("", "created_by", "varchar(8)"),
        ("", "start_time / end_time", "time"), ("", "estimated_cost", "bigint"), ("", "order_index", "smallint"),
        ("", "status", "varchar(10)"), ("FK", "from_proposal_id", "bigint")])
    _, alts = erd_table(d, "activity_alternatives", 950, 520, [
        ("PK", "id", "bigint"), ("FK", "activity_id", "bigint"), ("FK", "place_id", "bigint"),
        ("", "tier", "varchar(20)"), ("", "estimated_cost", "bigint"), ("", "is_selected", "boolean")])
    _, places = erd_table(d, "places", 950, 140, [
        ("PK", "id", "bigint"), ("UQ", "provider + external_id", "varchar"), ("", "name", "varchar(255)"),
        ("", "category", "varchar(64)"), ("", "latitude / longitude", "numeric"), ("", "rating", "numeric(2,1)"),
        ("", "price_level", "smallint"), ("", "adopted_via", "varchar(16)")])
    _, saved = erd_table(d, "saved_places", 950, -40, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "place_id", "bigint")])
    _, rating = erd_table(d, "place_ratings", 1260, 140, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "place_id", "bigint"), ("FK", "trip_id", "bigint"),
        ("", "verdict", "varchar(8)"), ("", "note", "varchar(200)")])
    _, exp = erd_table(d, "expenses", 640, -40, [
        ("PK", "id", "bigint"), ("FK", "trip_id", "bigint"), ("FK", "activity_id", "bigint"),
        ("", "category", "varchar(16)"), ("", "amount", "bigint"), ("", "currency", "char(3)"),
        ("", "expense_date", "date")])
    _, chk = erd_table(d, "trip_checklist_items", 330, 520, [
        ("PK", "id", "bigint"), ("FK", "trip_id", "bigint"), ("", "kind", "varchar(8)"), ("", "title", "varchar(120)"),
        ("", "category", "varchar(16)"), ("", "due_date", "date"), ("", "is_done", "boolean"), ("", "source", "varchar(10)")])
    erd_rel(d, pref["user_id"], users["id"], many="ERzeroToOne")
    erd_rel(d, trips["user_id"], users["id"])
    erd_rel(d, trips["destination_id"], dest["id"])
    erd_rel(d, days["trip_id"], trips["id"])
    erd_rel(d, acts["itinerary_day_id"], days["id"])
    erd_rel(d, acts["place_id"], places["id"], one="ERzeroToOne")
    erd_rel(d, alts["activity_id"], acts["id"])
    erd_rel(d, alts["place_id"], places["id"], one="ERzeroToOne")
    erd_rel(d, saved["place_id"], places["id"])
    erd_rel(d, saved["user_id"], users["id"], points=[(930, 21), (930, -60), (8, -60), (8, 59)],
            extra="exitX=0;exitY=0.5;entryX=0;entryY=0.5;")
    erd_rel(d, rating["place_id"], places["id"])
    erd_rel(d, exp["trip_id"], trips["id"])
    erd_rel(d, exp["activity_id"], acts["id"], one="ERzeroToOne")
    erd_rel(d, chk["trip_id"], trips["id"])
    d.save(SRC / "DGM-ERD-001-chuyen-di.drawio")


def erd_ai():
    d = Doc("ERD nhóm AI và cộng đồng")
    _, users = erd_table(d, "users", 20, 20, [("PK", "id", "bigint"), ("", "email", "varchar(255)"),
                                               ("", "name", "varchar(120)"), ("", "role", "varchar(16)")])
    _, trips = erd_table(d, "trips", 20, 200, [("PK", "id", "bigint"), ("FK", "user_id", "bigint"),
                                                ("FK", "destination_id", "bigint"), ("", "name", "varchar(160)")])
    _, dest = erd_table(d, "destinations", 20, 560, [("PK", "id", "bigint"), ("", "name", "varchar(160)"),
                                                      ("", "country", "varchar(80)")])
    _, conv = erd_table(d, "conversations", 330, 20, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "trip_id", "bigint"), ("", "title", "varchar(200)")])
    _, msg = erd_table(d, "messages", 640, -60, [
        ("PK", "id", "bigint"), ("FK", "conversation_id", "bigint"), ("", "role", "varchar(12)"), ("", "content", "text"),
        ("", "token_usage", "jsonb"), ("", "attachments_json", "jsonb")])
    _, prop = erd_table(d, "ai_proposals", 640, 140, [
        ("PK", "id", "bigint"), ("FK", "trip_id", "bigint"), ("FK", "conversation_id", "bigint"),
        ("FK", "message_id", "bigint"), ("", "summary", "text"), ("", "changes_json", "jsonb"),
        ("", "status", "varchar(16)"), ("", "undo_json", "jsonb"), ("", "expires_at", "timestamptz")])
    _, tool = erd_table(d, "ai_tool_executions", 950, -60, [
        ("PK", "id", "bigint"), ("FK", "conversation_id", "bigint"), ("FK", "message_id", "bigint"),
        ("FK", "trip_id", "bigint"), ("FK", "user_id", "bigint"), ("", "tool_name", "varchar(64)"),
        ("", "arguments / result", "jsonb"), ("", "status", "varchar(12)"), ("", "execution_time_ms", "int")])
    _, know = erd_table(d, "destination_knowledge", 330, 480, [
        ("PK", "id", "bigint"), ("FK", "destination_id", "bigint"), ("", "destination_name", "varchar(160)"),
        ("", "category", "varchar(32)"), ("", "title", "varchar(255)"), ("", "content", "text"),
        ("", "priority", "int"), ("", "is_active", "boolean")])
    _, post = erd_table(d, "blog_posts", 330, 200, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "trip_id", "bigint"),
        ("", "destination_name", "varchar(160)"), ("", "title", "varchar(255)"), ("", "content", "text"),
        ("", "photo_urls", "jsonb"), ("", "upvote_count", "int"), ("", "comment_count", "int"),
        ("FK", "converted_to_knowledge_id", "bigint")])
    _, cmt = erd_table(d, "blog_comments", 640, 400, [
        ("PK", "id", "bigint"), ("FK", "post_id", "bigint"), ("FK", "user_id", "bigint"), ("", "content", "text"),
        ("", "upvote_count", "int")])
    _, pup = erd_table(d, "blog_post_upvotes", 640, 570, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "post_id", "bigint")])
    _, cup = erd_table(d, "blog_comment_upvotes", 950, 400, [
        ("PK", "id", "bigint"), ("FK", "user_id", "bigint"), ("FK", "comment_id", "bigint")])
    erd_rel(d, trips["user_id"], users["id"])
    erd_rel(d, conv["user_id"], users["id"])
    erd_rel(d, conv["trip_id"], trips["id"])
    erd_rel(d, msg["conversation_id"], conv["id"])
    erd_rel(d, prop["conversation_id"], conv["id"])
    erd_rel(d, prop["message_id"], msg["id"], one="ERzeroToOne")
    erd_rel(d, tool["message_id"], msg["id"], one="ERzeroToOne")
    erd_rel(d, know["destination_id"], dest["id"], one="ERzeroToOne")
    erd_rel(d, post["converted_to_knowledge_id"], know["id"], one="ERzeroToOne")
    erd_rel(d, post["user_id"], users["id"])
    erd_rel(d, post["trip_id"], trips["id"], one="ERzeroToOne")
    erd_rel(d, cmt["post_id"], post["id"])
    erd_rel(d, pup["post_id"], post["id"])
    erd_rel(d, cup["comment_id"], cmt["id"])
    d.save(SRC / "DGM-ERD-002-ai-cong-dong.drawio")


def build():
    erd_core()
    erd_ai()
