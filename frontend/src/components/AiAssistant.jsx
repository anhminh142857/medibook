import { useEffect, useRef, useState } from "react";

const WELCOME_MESSAGE = {
  role: "assistant",
  content:
    "Xin chào! Tôi là Trợ lý MediBook. Bạn có thể hỏi về cách đặt lịch, hủy lịch và sử dụng website. Tôi không tư vấn chẩn đoán hoặc điều trị.",
};

const SUGGESTIONS = [
  "Làm sao để đặt lịch khám?",
  "Tôi có thể hủy lịch khi nào?",
  "Vì sao chưa thể hoàn thành lịch khám?",
];

export default function AiAssistant() {
  const [open, setOpen] = useState(false);
  const [messages, setMessages] = useState([WELCOME_MESSAGE]);
  const [question, setQuestion] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const bottomRef = useRef(null);
  const controllerRef = useRef(null);
  const requestIdRef = useRef(0);

  useEffect(() => {
    if (open) {
      bottomRef.current?.scrollIntoView({
        block: "nearest",
      });
    }
  }, [open, messages, loading, error]);

  useEffect(() => {
    function resetConversation() {
      requestIdRef.current += 1;
      controllerRef.current?.abort();

      setMessages([WELCOME_MESSAGE]);
      setQuestion("");
      setError("");
      setLoading(false);
      setOpen(false);
    }

    function handleStorage(event) {
      if (
        event.key === "token" ||
        event.key === "user" ||
        event.key === null
      ) {
        resetConversation();
      }
    }

    window.addEventListener("auth-changed", resetConversation);
    window.addEventListener("storage", handleStorage);

    return () => {
      window.removeEventListener("auth-changed", resetConversation);
      window.removeEventListener("storage", handleStorage);

      requestIdRef.current += 1;
      controllerRef.current?.abort();
    };
  }, []);

  async function sendQuestion(value) {
    const text = value.trim();

    if (!text || loading) {
      return;
    }

    if (text.length > 1000) {
      setError("Câu hỏi không được vượt quá 1000 ký tự.");
      return;
    }

    const token = localStorage.getItem("token");

    if (
      !token ||
      token === "null" ||
      token === "undefined" ||
      !token.trim()
    ) {
      setError("Vui lòng đăng nhập trước khi sử dụng trợ lý.");
      return;
    }

    const requestId = ++requestIdRef.current;
    const controller = new AbortController();

    controllerRef.current = controller;

    let timedOut = false;

    const timeoutId = window.setTimeout(() => {
      timedOut = true;
      controller.abort();
    }, 120000);

    setError("");
    setLoading(true);
    setQuestion("");

    setMessages((previous) => [
      ...previous,
      {
        role: "user",
        content: text,
      },
    ]);

    try {
      const response = await fetch(
        "http://localhost:8080/api/ai/chat",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            message: text,
          }),
          signal: controller.signal,
        }
      );

      if (!response.ok) {
        const body = await response.json().catch(() => null);

        if (response.status === 401) {
          throw new Error(
            "Phiên đăng nhập đã hết hạn. Vui lòng đăng xuất và đăng nhập lại."
          );
        }

        if (response.status === 403) {
          throw new Error(
            "Tài khoản không có quyền sử dụng chức năng này."
          );
        }

        if (response.status === 429) {
          throw new Error(
            "Trợ lý đang xử lý câu hỏi khác. Vui lòng chờ rồi thử lại."
          );
        }

        if (response.status === 503) {
          throw new Error(
            "Trợ lý chưa sẵn sàng. Kiểm tra Ollama đang chạy và backend đã bật AI."
          );
        }

        const serverMessage =
          typeof body?.message === "string"
            ? body.message
            : typeof body?.detail === "string"
              ? body.detail
              : "";

        throw new Error(
          serverMessage ||
            `Không gửi được câu hỏi (HTTP ${response.status}).`
        );
      }

      const data = await response.json();

      if (
        typeof data?.answer !== "string" ||
        !data.answer.trim()
      ) {
        throw new Error("Trợ lý chưa trả về câu trả lời hợp lệ.");
      }

      if (requestId !== requestIdRef.current) {
        return;
      }

      setMessages((previous) => [
        ...previous,
        {
          role: "assistant",
          content: data.answer.trim(),
        },
      ]);
    } catch (err) {
      if (requestId !== requestIdRef.current) {
        return;
      }

      if (err.name === "AbortError") {
        if (timedOut) {
          setError(
            "Trợ lý phản hồi quá lâu. Vui lòng chờ một chút rồi thử lại."
          );
          setQuestion(text);
        }

        return;
      }

      setError(
        err instanceof TypeError
          ? "Không kết nối được backend. Kiểm tra backend đang chạy tại cổng 8080."
          : err.message || "Có lỗi xảy ra. Vui lòng thử lại."
      );

      setQuestion(text);
    } finally {
      window.clearTimeout(timeoutId);

      if (requestId === requestIdRef.current) {
        setLoading(false);
        controllerRef.current = null;
      }
    }
  }

  function handleSubmit(event) {
    event.preventDefault();
    sendQuestion(question);
  }

  return (
    <>
      <button
        type="button"
        className="btn btn-primary medibook-ai-toggle shadow"
        aria-expanded={open}
        aria-controls="medibook-ai-panel"
        onClick={() => setOpen((previous) => !previous)}
      >
        {open ? "Đóng trợ lý" : "Trợ lý MediBook"}
      </button>

      {open && (
        <section
          id="medibook-ai-panel"
          className="medibook-ai-panel"
          aria-labelledby="medibook-ai-title"
        >
          <header className="medibook-ai-header">
            <div>
              <h2 id="medibook-ai-title">
                Trợ lý MediBook
              </h2>
              <p>Hướng dẫn sử dụng website bằng AI</p>
            </div>

            <button
              type="button"
              className="btn btn-sm btn-outline-light"
              aria-label="Đóng khung trợ lý"
              onClick={() => setOpen(false)}
            >
              Đóng
            </button>
          </header>

          <div className="medibook-ai-notice">
            Không nhập mật khẩu hoặc thông tin y tế riêng.
            Mỗi câu hỏi được xử lý độc lập. AI có thể trả lời sai.
          </div>

          <div className="medibook-ai-messages">
            <div
              role="log"
              aria-live="polite"
              aria-relevant="additions"
              aria-label="Hội thoại với trợ lý"
            >
              {messages.map((message, index) => (
                <div
                  key={index}
                  className={`medibook-ai-message ${
                    message.role === "user"
                      ? "medibook-ai-message-user"
                      : "medibook-ai-message-assistant"
                  }`}
                >
                  <div className="medibook-ai-message-label">
                    {message.role === "user"
                      ? "Bạn"
                      : "Trợ lý MediBook"}
                  </div>

                  <div>{message.content}</div>
                </div>
              ))}
            </div>

            {loading && (
              <div
                className="medibook-ai-loading"
                role="status"
              >
                <span
                  className="spinner-border spinner-border-sm me-2"
                  aria-hidden="true"
                />
                Đang trả lời, lần đầu có thể cần chờ lâu hơn...
              </div>
            )}

            {error && (
              <div
                className="alert alert-danger small mb-0 mt-3"
                role="alert"
              >
                {error}
              </div>
            )}

            <div ref={bottomRef} />
          </div>

          <div className="medibook-ai-suggestions">
            {SUGGESTIONS.map((suggestion) => (
              <button
                type="button"
                key={suggestion}
                className="btn btn-sm btn-outline-primary"
                disabled={loading}
                onClick={() => sendQuestion(suggestion)}
              >
                {suggestion}
              </button>
            ))}
          </div>

          <form
            className="medibook-ai-form"
            onSubmit={handleSubmit}
          >
            <label
              htmlFor="medibook-ai-question"
              className="visually-hidden"
            >
              Câu hỏi dành cho trợ lý
            </label>

            <textarea
              id="medibook-ai-question"
              className="form-control"
              rows={2}
              maxLength={1000}
              value={question}
              disabled={loading}
              onChange={(event) =>
                setQuestion(event.target.value)
              }
              placeholder="Ví dụ: Tôi có thể hủy lịch khi nào?"
              required
            />

            <div className="d-flex justify-content-between align-items-center mt-2">
              <span className="small text-muted">
                {question.length}/1000 ký tự
              </span>

              <button
                type="submit"
                className="btn btn-primary"
                disabled={loading || !question.trim()}
              >
                {loading ? "Đang gửi..." : "Gửi câu hỏi"}
              </button>
            </div>
          </form>
        </section>
      )}
    </>
  );
}