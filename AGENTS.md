# Taste Skill: Professional Design & Motion Directives (Leonxlnx Guidelines)

This project strictly adheres to the design philosophy and rules formulated in **Leonxlnx/taste-skill** to ensure every UI element, motion graphic, and typography hierarchy exudes genuine professional craftsmanship, avoiding generic "AI slop" or default templates.

---

## 🎛️ Tunable Parameters (Active Configuration)

- **`DESIGN_VARIANCE = 8/10`**: High visual boldness, asymmetric industrial depth, intentional hierarchy, specialized monospace and display pairing.
- **`MOTION_INTENSITY = 9/10`**: Physics-based springs (`spring(dampingRatio, stiffness)`), multi-axis cascading delays (Ramotion Garland style), 3D multi-fold origami hinges (Folding Cell style), 60FPS fluid canvas shaders.
- **`VISUAL_DENSITY = 8.5/10`**: Industrial telemetry grade. Compact, high-information-density layout with crisp micro-labels, tolerances, upper/lower limit markers, and live telemetry badges.

---

## 🎨 Core Design Directives (from taste-skill)

### 1. Zero AI Slop (Anti-Generic Mandate)
- **NO** plain gray cards, boring purple-on-white gradients, or unstyled flat lists.
- **YES** to deep, premium dark surfaces (`#0D0E10`, `#131417`, `#1A1C20`) paired with subtle structural borders (`1.dp` @ `Color.White.copy(alpha = 0.06f ~ 0.08f)`).
- **Atmosphere & Depth**: Employ layered glassmorphism, radial gradient glows, HUD crosshair brackets, and dynamic canvas shaders instead of static flat containers.

### 2. Typographic Hierarchy & Precision
- **Data & Telemetry**: Always format numbers, dimensions, tolerances, and furnace IDs with crisp **Monospace typography** (`FontFamily.Monospace`) and generous letter spacing (`letterSpacing = 1.sp ~ 1.5.sp`).
- **Headings vs Badges**: Strong weight contrast (`FontWeight.Bold` vs `FontWeight.Medium` subtitle / `FontWeight.Light` caption).

### 3. Motion & Micro-Interactions (Springs & 3D Staggering)
- Avoid linear transitions; enforce damped springs (`dampingRatio = Spring.DampingRatioMediumBouncy` or `0.78f`).
- Respect multi-stage user actions: Collapsed state (clean, uncluttered) ➔ Cascading 3D reveal with lighting and depth shadows ➔ Smooth spring bounce return.
- Never show non-functional dead UI. Every interactive element must provide tactile visual/haptic feedback (`interactionSource`, scale shrink `0.96f` on press, dynamic glow).

### 4. Accessibility & Safe-Area Discipline
- Minimum touch target: 48dp x 48dp (`minimumInteractiveComponentSize()`).
- Strict edge-to-edge adherence (`WindowInsets.safeDrawing`, `navigationBarsPadding()`).
