# Transaction Proxy Service

Проксі-сервіс для API https://api.spending.gov.ua/api/v2/api/transactions з додаванням SHA256 хешів та оптимізацією пам'яті.

## Особливості

- ✅ **SHA256 хеші** - кожна транзакція містить унікальний хеш
- ✅ **Пагінація** - підтримка великих обсягів даних з пагінацією
- ✅ **Кешування** - локальне кешування для оптимізації продуктивності
- ✅ **Обмеження пам'яті** - оптимізація для роботи з обмеженням 512MB
- ✅ **Обробка 1M+ транзакцій** - ефективна обробка великих обсягів даних
- ✅ **Моніторинг пам'яті** - автоматичне очищення кешу при високому використанні
- ✅ **Обробка помилок** - комплексна обробка всіх типів помилок

## Технічний стек

- **Java 21**
- **Spring Boot 3.x**
- **Spring WebFlux** (реактивне програмування)
- **Spring Data JPA** + **Postgresql** (для кешування)
- **Netty** (HTTP клієнт)
- **JUnit 5** + **Mockito** (тестування)

## Архітектура

### Основні компоненти:

1. **TransactionController** - REST контролер з пагінацією
2. **TransactionService** - бізнес-логіка з реактивною обробкою
3. **TransactionCacheService** - сервіс кешування з оптимізацією пам'яті
4. **MemoryMonitoringService** - моніторинг та автоматичне управління пам'яттю
5. **HashUtil** - генерація SHA256 хешів

### Оптимізації пам'яті:

- Максимум 5000 транзакцій на запит
- Паралелізм обробки обмежений до 8 потоків
- Автоматичне очищення старого кешу
- Стиснення HTTP відповідей
- Оптимізований розмір буферів WebClient

## API

### GET /api/v2/api/transactions

Отримання транзакцій з пагінацією та хешуванням.

#### Параметри:

- `recipt_edrpous` (required) - список ЄДРПОУ отримувачів
- `startdate` (required) - дата початку (YYYY-MM-DD)
- `enddate` (required) - дата кінця (YYYY-MM-DD)
- `page` (optional) - номер сторінки (default: 0)
- `size` (optional) - розмір сторінки (default: 1000, max: 2000)

#### Приклади запитів:

```bash
# Основний запит з пагінацією
GET /api/v2/api/transactions?recipt_edrpous=00013480&startdate=2024-10-29&enddate=2024-10-31&page=0&size=100

# Множинні ЄДРПОУ
GET /api/v2/api/transactions?recipt_edrpous=14360570&recipt_edrpous=21560766&startdate=2024-10-01&enddate=2024-10-31
```

#### Відповідь:

```json
{
  "transactions": [
    {
      "transaction": {
        "id": 123456,
        "doc_vob": "Видаток",
        "amount": 1000.0,
        "currency": "UAH",
        "recipt_edrpou": "00013480",
        "recipt_name": "Назва отримувача",
        // ... інші поля транзакції
      },
      "hash": "7d865e959b2466918c9863afca942d0fb89d7c9ac0c99bafc3749504ded97730"
    }
  ],
  "page": {
    "currentPage": 0,
    "pageSize": 100,
    "totalElements": 1500,
    "totalPages": 15,
    "hasNext": true,
    "hasPrevious": false
  }
}
```

## Запуск

### Локальний запуск:

```bash
# Клонування репозиторію
git clone <repository-url>
cd transaction-proxy

# Запуск з обмеженням пам'яті
java -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -jar target/transaction-proxy.jar
```

### Docker:

```dockerfile
FROM openjdk:21-jre-slim
COPY target/transaction-proxy.jar app.jar
ENTRYPOINT ["java", "-Xmx512m", "-XX:+UseG1GC", "-jar", "/app.jar"]
```

### Властивості системи:

Додайте до запуску JVM для оптимізації пам'яті:
- `-Xmx512m` - максимум 512MB heap
- `-XX:+UseG1GC` - G1 збирач сміття для кращої роботи з великими обсягами
- `-XX:MaxGCPauseMillis=200` - цільова пауза GC
- `-XX:+UseStringDeduplication` - дедуплікація рядків

## Тестування

### Запуск тестів:

```bash
mvn test
```

### Типи тестів:

1. **Unit тести** - тестування сервісів та утиліт
2. **Integration тести** - тестування HTTP ендпоінтів


## Моніторинг

Сервіс автоматично моніторить використання пам'яті та логує:

- Поточне використання пам'яті кожну хвилину
- Детальну статистику кожні 5 хвилин
- Автоматичне очищення кешу при досягненні 80% використання пам'яті

## Особливості реалізації

### Хешування:
- Використовується SHA256 для кожної транзакції
- Хеш генерується на основі повного JSON об'єкту
- Ідентичні транзакції мають ідентичні хеші

### Кешування:
- Локальне кешування в Postgresql базі даних
- Перевірка існуючих записів перед зверненням до API
- Автоматичне очищення старих записів

### Обмеження пам'яті:
- Максимум 5000 транзакцій на запит
- Оптимізовані розміри буферів
- Реактивна обробка з backpressure
- Автоматичне управління пам'яттю

## Конфігурація

Основні налаштування в `application.yml`:


## Помилки та їх обробка

- **502 Bad Gateway** - помилка зовнішнього API
- **500 Internal Server Error** - внутрішні помилки обробки
- **503 Service Unavailable** - недоступність зовнішнього сервісу

## Продуктивність

При правильному налаштування сервіс здатний:
- Обробляти понад 1 мільйон транзакцій
- Працювати в межах 512MB пам'яті
- Підтримувати високу швидкість відповіді завдяки кешуванню
- Автоматично масштабуватися під навантаженням

