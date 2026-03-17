# Fast OpenAI SDK (Java)

A lightweight, high-performance Java SDK for the OpenAI API.
This SDK is designed for ease of use, with built-in support for stream events, load balancing across multiple API keys, request retries, and proxy configuration.

## Features
- **OpenAI Style API**: Fully compatible with OpenAI's REST API.
- **Streaming Support**: Built-in support for Server-Sent Events (SSE) for chat completions.
- **Multiple Keys & Load Balancing**: Configure multiple API keys to automatically load balance requests.
- **Configuration File**: Manage `apiKeys`, `baseUrl`, timeouts, and proxies through standard `fast-openai.properties`.
- **Extensible**: Highly extensible via interceptors and callbacks.

## Quick Start

### 1. Configuration

Create `fast-openai.properties` in your `src/main/resources`:

```properties
# OpenAI API Keys, comma separated for load balancing
openai.api.keys=sk-your-key-here,sk-another-key-here
# OpenAI Base URL
openai.api.baseUrl=https://api.openai.com/
# Request Timeout in seconds
openai.api.timeout=60
# Max retries
openai.api.maxRetries=3
# Optional Proxy configuration
# openai.proxy.host=127.0.0.1
# openai.proxy.port=7890
# openai.proxy.type=HTTP
```

### 2. Usage Example

```java
import com.fast.openai.api.OpenAiService;
import com.fast.openai.config.OpenAiConfig;
import com.fast.openai.request.ChatRequest;
import com.fast.openai.request.dto.Message;
import com.fast.openai.result.ChatResult;

public class Main {
    public static void main(String[] args) {
        // Automatically load config from fast-openai.properties
        OpenAiConfig config = OpenAiConfig.loadFromProperties();
        OpenAiService service = new OpenAiService(config);

        ChatRequest request = ChatRequest.builder()
                .model("gpt-4o")
                .messages(List.of(Message.ofUser("Hello!")))
                .build();

        ChatResult result = service.chat(request);
        System.out.println(result.getChoices().get(0).getMessage().getContent());
    }
}
```

## Contributing
Contributions are welcome! Please feel free to submit a Pull Request.

## License
MIT License
