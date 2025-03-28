Refactor the following code by **removing all magic numbers and hardcoded configuration values**. Replace them with named constants loaded from **environment variables**. Load .env file in the code.

---
[insert code here]

---

🔧 **Instructions:**
• Identify all magic numbers, strings, or hardcoded values that represent settings, thresholds, or tunables.
• Move these values into a config structure (like a .env  or dictionary at the top of the file).
• Use clear, descriptive names for each setting.
• Maintain the same behavior — do **not** change the logic.
• Output the full code in a **single file**, even if it’s long.
• If needed, include an example of the configuration structure at the top or bottom of the file.

📦 Bonus (if applicable):
• Support both environment variable loading (os.environ.get()) and a fallback default value.
• Group related config values under logical sections or prefixes.