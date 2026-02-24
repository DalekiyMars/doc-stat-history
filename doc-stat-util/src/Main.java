import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Main {

    private static final String DEFAULT_CONFIG_FILE = "doc-stat-util/src/config.txt";

    public static void main(String[] args) {
        // Определяем файл конфигурации
        String configFile = args.length > 0 ? args[0] : DEFAULT_CONFIG_FILE;
        Path configPath = Paths.get(configFile);

        if (!Files.exists(configPath)) {
            System.err.println("Файл конфигурации не найден: " + configPath.toAbsolutePath());
            System.exit(1);
        }

        // Параметры
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(configPath)) {
            props.load(is);
        } catch (IOException e) {
            System.err.println("Ошибка чтения файла конфигурации: " + e.getMessage());
            System.exit(1);
        }

        String serviceUrl = props.getProperty("service.url", "http://localhost:8080");
        int count;
        int batchSize;
        try {
            count = Integer.parseInt(props.getProperty("generator.count", "100"));
            batchSize = Integer.parseInt(props.getProperty("generator.batchSize", "100"));
        } catch (NumberFormatException e) {
            System.err.println("Неверное число в generator.count или generator.batchSize");
            System.exit(1);
            return;
        }
        String initiator = props.getProperty("generator.initiator", "util-generator");
        String author = props.getProperty("generator.author", "util");
        String docNamePrefix = props.getProperty("generator.docNamePrefix", "Generated Document");

        System.out.println("=== Запуск пакетной генерации документов ===" +
                "Задано к созданию: " + count + " документов" +
                "Размер пачки: " + batchSize +
                "Сервис: " + serviceUrl +
                "Инициатор: " + initiator +
                "Автор: " + author +
                "Префикс имени: " + docNamePrefix);

        HttpClient client = HttpClient.newHttpClient();

        int totalSuccess = 0;
        int totalFail = 0;
        int batchesProcessed = 0;
        Instant totalStart = Instant.now();

        // Разбиваем на пачки и отправляем
        for (int start = 1; start <= count; start += batchSize) {
            int end = Math.min(start + batchSize - 1, count);
            int currentBatchSize = end - start + 1;
            batchesProcessed++;

            // Формируем список документов для текущей пачки
            List<CreateDocumentRequest> documents = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                String docName = docNamePrefix + " #" + i;
                documents.add(new CreateDocumentRequest(author, docName, initiator));
            }

            BatchCreateRequest batchRequest = new BatchCreateRequest(documents);
            String jsonBody = toJson(batchRequest);

            // Отправляем пачку
            Instant batchStart = Instant.now();
            int successInBatch = sendBatchCreateRequest(client, serviceUrl, jsonBody);
            Duration batchDuration = Duration.between(batchStart, Instant.now());

            totalSuccess += successInBatch;
            int failInBatch = currentBatchSize - successInBatch;
            totalFail += failInBatch;

            System.out.printf("Пачка %d (%d док.): успешно %d, ошибок %d, время %d мс%n",
                    batchesProcessed, currentBatchSize, successInBatch, failInBatch,
                    batchDuration.toMillis());

            System.out.printf("Прогресс: %d/%d документов обработано.%n", end, count);
        }

        Duration totalDuration = Duration.between(totalStart, Instant.now());
        System.out.println("=== Генерация завершена ===");
        System.out.printf("Всего пачек: %d%n", batchesProcessed);
        System.out.printf("Общее время: %d мс%n", totalDuration.toMillis());
        System.out.printf("Успешно: %d, ошибок: %d%n", totalSuccess, totalFail);
    }

    /**
     * Отправляет POST-запрос на пакетное создание документов.
     * @return количество успешно созданных документов (по ответу сервера)
     */
    private static int sendBatchCreateRequest(HttpClient client, String baseUrl, String jsonBody) {
        String url = baseUrl + (baseUrl.endsWith("/") ? "" : "/") + "api/v1/documents/batch";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 201) {
                return parseSuccessCount(response.body());
            } else {
                System.err.println("Ошибка batch-запроса, статус: " + response.statusCode());
                return 0;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Ошибка HTTP-запроса: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Парсинг JSON-ответа для получения количества созданных документов.
     * Ожидается, что ответ — массив объектов DocumentResponse.
     */
    private static int parseSuccessCount(String jsonResponse) {
        if (jsonResponse == null || jsonResponse.trim().isEmpty()) return 0;
        int count = 0;
        boolean inObject = false;
        for (char c : jsonResponse.toCharArray()) {
            if (c == '{') {
                if (!inObject) {
                    count++;
                    inObject = true;
                }
            } else if (c == '}') {
                inObject = false;
            }
        }
        return count;
    }

    /**
     * Преобразование объекта в JSON.
     */
    private static String toJson(Object obj) {
        if (obj instanceof BatchCreateRequest batch) {
            StringBuilder sb = new StringBuilder("{\"documents\":[");
            for (int i = 0; i < batch.documents().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(toJson(batch.documents().get(i)));
            }
            sb.append("]}");
            return sb.toString();
        } else if (obj instanceof CreateDocumentRequest doc) {
            return String.format("{\"author\":\"%s\",\"docName\":\"%s\",\"initiator\":\"%s\"}",
                    escapeJson(doc.author()), escapeJson(doc.docName()), escapeJson(doc.initiator()));
        }
        return "{}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record CreateDocumentRequest(String author, String docName, String initiator) {}
    private record BatchCreateRequest(List<CreateDocumentRequest> documents) {}
}