The following code has **poor database connection handling**. Your task is to **improve resource management** by ensuring database connections are handled safely, efficiently, and correctly.

---
[insert code here]

---

🔧 **Instructions:**
• Ensure all database connections are **properly opened and closed** using safe patterns (e.g., context managers, try/finally blocks, connection pooling).
• Prevent resource leaks or hanging connections.
• Use connection pooling or a centralized connection manager if appropriate for the environment.
• Avoid opening new connections unnecessarily — reuse when possible.
• Focus only on **connection management** — do **not** modify queries, logic, or other parts of the code.
• Output the complete updated code in a **single file**.
• If the output is too long, **continue until the full code is included**.