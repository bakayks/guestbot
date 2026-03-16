# GuestBot Project Memory

## Architecture
- Java/Spring Boot multi-module Maven project
- Modules: guestbot-core, guestbot-repository, guestbot-service, guestbot-api, guestbot-telegram, guestbot-app
- DB: PostgreSQL + Flyway migrations (V1, V2)
- Cache: Redis (sessions)
- Config: guestbot-app/src/main/resources/application.yml

## Telegram Module — Marketplace Bot Architecture
- **Один shared bot token** в конфиге: `telegram.bot-token: ${TELEGRAM_BOT_TOKEN}`
- Бот — агрегатор/маркетплейс: гость описывает пожелания, бот рекомендует отели
- Гость НЕ знает заранее какой отель. Флоу: /start → пожелания → список отелей → выбор → бронирование
- У Hotel нет поля telegramBotToken и telegramLinkCode
- `botActive=true` означает что отель участвует в боте

## Session Flow
- Redis key: `session:{chatId}` (keyed by chatId only)
- TTL: 2h, max history: 20 messages
- `SessionManager.getOrCreate(chatId)` — создаёт сессию БЕЗ hotelId (null изначально)
- `SessionManager.setHotel(chatId, hotelId)` — привязывает отель после выбора + сбрасывает state в IDLE
- ConversationSession: hotelId (nullable), checkIn, checkOut, guestName, guestPhone, pendingBookingId, history

## SessionState Flow
IDLE → (нет hotelId) → handleDiscovery → SELECTING_HOTEL → handleHotelSelection → IDLE
IDLE → (есть hotelId) → AiHandler.handle
IDLE → COLLECTING_GUEST_NAME → COLLECTING_GUEST_PHONE → COLLECTING_CHECK_IN → COLLECTING_CHECK_OUT → AWAITING_PAYMENT

## Key Routing (MessageHandler)
- hotel == null && state != SELECTING_HOTEL → AiHandler.handleDiscovery
- hotel == null && state == SELECTING_HOTEL → AiHandler.handleHotelSelection
- hotel != null && collecting booking data → BookingFlowHandler
- hotel != null && otherwise → AiHandler.handle

## Key File Locations
- Hotel entity: guestbot-core/.../entity/Hotel.java (botActive field)
- HotelRepository: findByBotActiveTrueAndActiveTrue()
- HotelService: getActiveBotHotels(), getById(), getByOwner()
- UpdateDispatcher: loads session, resolves hotel if hotelId in session, passes to MessageHandler
- TelegramClient: uses shared botToken from @Value, WebClient singleton
- TelegramNotificationService: uses shared botToken from @Value
- TelegramEventListener: onOwnerReplied now works correctly

## TODOs in codebase
- AiHandler: Claude API integration (handleDiscovery, handle)
- BookingFlowHandler.handleCheckOut: call BookingService.create(...)
- UpdateDispatcher: callback_query handling
