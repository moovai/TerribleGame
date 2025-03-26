# Refactoring Plan for TerribleGame.java

**Goal:** Improve code structure, maintainability, security, and readability by applying Object-Oriented principles and separating concerns.

## Analysis of Issues

1.  **God Class:** `TerribleGame` handles UI, game logic, input, database, and rendering.
2.  **Lack of OOP:** Entities (Player, Enemy, etc.) use `int[]` instead of classes.
3.  **Mixed Concerns:** UI, game logic, input, and persistence are intertwined.
4.  **Database Security:** SQL injection vulnerabilities and plaintext passwords.
5.  **Magic Numbers:** Literal values used directly in code.
6.  **Concurrency:** Potential issues between game loop and rendering thread; suppressed exceptions.
7.  **State Management:** Poor encapsulation of game state variables.
8.  **Resource Management:** Database connection handling can be improved.

## Refactoring Phases

### Phase 1: Introduce Game Entity Classes

*   **Step 1.1: Create `GameObject` Base Class/Interface:**
    *   Define common properties (`x`, `y`, `width`, `height`) and methods (`update()`, `draw(Graphics g)`, `getBounds()`).
*   **Step 1.2: Create `Player` Class:**
    *   Inherit/implement `GameObject`.
    *   Encapsulate player state (`playerX`, `playerY`, etc.) and movement/firing methods.
*   **Step 1.3: Create `Enemy` Class:**
    *   Inherit/implement `GameObject`.
    *   Encapsulate enemy state and movement logic.
*   **Step 1.4: Create `Bullet` Class:**
    *   Inherit/implement `GameObject`.
    *   Encapsulate bullet state and movement logic.
*   **Step 1.5: Create `PowerUp` Class:**
    *   Inherit/implement `GameObject`.
    *   Encapsulate power-up state and logic.
*   **Step 1.6: Update `TerribleGame` and `GamePanel`:**
    *   Replace `ArrayList<int[]>` with `ArrayList<EntityClass>`.
    *   Update logic and rendering to use new object methods/properties.

### Phase 2: Separate Concerns

*   **Step 2.1: Create `DatabaseManager` Class:**
    *   Move all database methods here.
    *   Encapsulate the `Connection` object.
*   **Step 2.2: Create `GameState` Class:**
    *   Move game state variables (`score`, `lives`, lists of objects, etc.) here.
    *   Provide safe accessors/mutators.
*   **Step 2.3: Create `GameLogic` Class:**
    *   Move core game update logic (`updateGame`, spawning, collision) here.
    *   Operates on `GameState`.
*   **Step 2.4: Create `InputHandler` Class:**
    *   Implement `KeyListener`.
    *   Move input processing logic here.
    *   Decouple input from direct state modification.
*   **Step 2.5: Refactor `GamePanel`:**
    *   Make it a standalone class.
    *   Reads data only from `GameState` for rendering.
*   **Step 2.6: Refactor `LoginPanel` (Optional):**
    *   Make it a standalone class.
    *   Encapsulate login UI and logic, interacting with `DatabaseManager`.
*   **Step 2.7: Slim Down `TerribleGame` Class:**
    *   Reduce to an orchestrator role: creating components, managing panels, handling the main timer and window events.

### Phase 3: Address Security and Refinements

*   **Step 3.1: Implement `PreparedStatement`:**
    *   Modify `DatabaseManager` to use `PreparedStatement` for all user-input queries (prevent SQL injection).
*   **Step 3.2: Implement Password Hashing:**
    *   Modify `DatabaseManager` to hash passwords (e.g., bcrypt) before storing and compare hashes during login.
*   **Step 3.3: Centralize Constants:**
    *   Create a `Constants` class/interface for magic numbers.
*   **Step 3.4: Review and Refine Collision Detection:**
    *   Ensure collision logic (in `GameLogic`) is clear and uses `getBounds()`.
*   **Step 3.5: Improve Concurrency Handling:**
    *   Ensure safe state modification/reading between threads. Remove broad `try-catch(Exception)`.
*   **Step 3.6: Enhance Error Handling and Logging:**
    *   Use specific exceptions and a proper logging framework.
