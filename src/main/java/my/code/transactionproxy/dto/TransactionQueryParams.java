package my.code.transactionproxy.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Клас для представлення параметрів запиту транзакцій, включаючи фільтри та пагінацію.
 */
@Data // Генерує гетери, сетери, методи equals, hashCode та toString (Lombok).
@Builder // Додає патерн Builder для зручного створення об’єктів (Lombok).
public class TransactionQueryParams {
    // Список кодів ЄДРПОУ для фільтрації транзакцій.
    private List<String> edrpous; // Зберігає список кодів ЄДРПОУ (ідентифікатори організацій).

    // Початкова дата для фільтрації транзакцій.
    private LocalDate startDate; // Вказує початкову дату діапазону для пошуку транзакцій.

    // Кінцева дата для фільтрації транзакцій.
    private LocalDate endDate; // Вказує кінцеву дату діапазону для пошуку транзакцій.

    // Номер сторінки для пагінації (за замовчуванням 0).
    @Builder.Default // Вказує значення за замовчуванням для поля при використанні Builder (Lombok).
    private int page = 0; // Зберігає номер сторінки для пагінації (починається з 0).

    // Розмір сторінки для пагінації (за замовчуванням 1000).
    @Builder.Default // Вказує значення за замовчуванням для поля при використанні Builder (Lombok).
    private int size = 1000; // Зберігає кількість записів на одній сторінці.

    // Максимально допустимий розмір сторінки.
    private static final int MAX_PAGE_SIZE = 2000; // Встановлює верхню межу розміру сторінки (2000 записів).

    // Мінімально допустимий розмір сторінки.
    private static final int MIN_PAGE_SIZE = 1; // Встановлює нижню межу розміру сторінки (1 запис).

    /**
     * Створює об’єкт TransactionQueryParams із параметрів запиту у форматі MultiValueMap.
     * @param params Параметри запиту у форматі MultiValueMap.
     * @return Налаштований об’єкт TransactionQueryParams.
     */
    public static TransactionQueryParams fromMultiValueMap(MultiValueMap<String, String> params) {
        // Отримує список кодів ЄДРПОУ з параметрів запиту.
        List<String> edrpous = params.get("recipt_edrpous"); // Витягує значення параметра "recipt_edrpous" як список.

        // Парсить початкову дату з параметра "startdate", перевіряючи, що значення не null.
        LocalDate startDate = LocalDate.parse(requireNonNull(params.getFirst("startdate"))); // Конвертує рядок у LocalDate.

        // Парсить кінцеву дату з параметра "enddate", перевіряючи, що значення не null.
        LocalDate endDate = LocalDate.parse(requireNonNull(params.getFirst("enddate"))); // Конвертує рядок у LocalDate.

        // Отримує номер сторінки з параметра "page" або використовує 0, якщо параметр відсутній.
        int page = params.getFirst("page") != null ?
                Math.max(0, Integer.parseInt(requireNonNull(params.getFirst("page")))) : 0; // Парсить "page", забезпечуючи, що значення не менше 0.

        // Отримує розмір сторінки з параметра "size" або використовує 1000, якщо параметр відсутній.
        int size = params.getFirst("size") != null ?
                Integer.parseInt(requireNonNull(params.getFirst("size"))) : 1000; // Парсить "size" або використовує значення за замовчуванням.

        // Обмежує розмір сторінки в межах MIN_PAGE_SIZE і MAX_PAGE_SIZE.
        size = Math.max(MIN_PAGE_SIZE, Math.min(size, MAX_PAGE_SIZE)); // Гарантує, що розмір сторінки в допустимих межах.

        // Створює об’єкт TransactionQueryParams за допомогою Builder.
        return TransactionQueryParams.builder() // Ініціює Builder для створення об’єкта.
                .edrpous(edrpous) // Встановлює список ЄДРПОУ.
                .startDate(startDate) // Встановлює початкову дату.
                .endDate(endDate) // Встановлює кінцеву дату.
                .page(page) // Встановлює номер сторінки.
                .size(size) // Встановлює розмір сторінки.
                .build(); // Повертає готовий об’єкт TransactionQueryParams.
    }

    /**
     * Повертає розмір сторінки, обмежений допустимими межами.
     * @return Розмір сторінки в межах MIN_PAGE_SIZE та MAX_PAGE_SIZE.
     */
    public int getSize() {
        // Гарантує, що розмір сторінки завжди в межах допустимих значень.
        return Math.max(MIN_PAGE_SIZE, Math.min(size, MAX_PAGE_SIZE)); // Повертає розмір, обмежений MIN_PAGE_SIZE і MAX_PAGE_SIZE.
    }
}
