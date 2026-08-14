---
name: Homely Android
description: A calm, spatial smart-home interface where live domestic light and trusted safety state share one floor plan.
colors:
  primary: "#195E58"
  on-primary: "#FFFFFF"
  primary-container: "#A8F2E9"
  secondary-live: "#805610"
  secondary-live-container: "#FFDDA4"
  tertiary-foliage: "#53643E"
  safety: "#9D2E2E"
  safety-container: "#FFDAD6"
  plaster-background: "#F8F5EC"
  paper-surface: "#FFFCF3"
  architectural-ink: "#1C1C18"
  quiet-outline: "#757870"
  night-background: "#141512"
  night-surface: "#1B1C18"
  night-primary: "#8BD5CD"
  night-live: "#F3BD62"
  night-ink: "#E6E2D9"
typography:
  display:
    fontFamily: "Android system sans-serif"
    fontSize: "36sp"
    fontWeight: 600
    lineHeight: "42sp"
    letterSpacing: "-0.6sp"
  headline:
    fontFamily: "Android system sans-serif"
    fontSize: "28sp"
    fontWeight: 600
    lineHeight: "34sp"
    letterSpacing: "-0.25sp"
  title:
    fontFamily: "Android system sans-serif"
    fontSize: "22sp"
    fontWeight: 600
    lineHeight: "28sp"
  body:
    fontFamily: "Android system sans-serif"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
  label:
    fontFamily: "Android system sans-serif"
    fontSize: "14sp"
    fontWeight: 600
    lineHeight: "20sp"
rounded:
  card: "12dp"
  plan-inner: "18dp"
  plan: "24dp"
  sheet: "28dp"
  full: "999dp"
spacing:
  base: "4dp"
  xs: "8dp"
  sm: "12dp"
  md: "16dp"
  lg: "20dp"
  xl: "24dp"
  xxl: "32dp"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.on-primary}"
    typography: "{typography.label}"
    rounded: "{rounded.full}"
    padding: "12dp 24dp"
    height: "48dp"
  device-marker:
    backgroundColor: "{colors.paper-surface}"
    textColor: "{colors.architectural-ink}"
    typography: "{typography.label}"
    rounded: "{rounded.full}"
    size: "48dp"
  floor-plan:
    backgroundColor: "{colors.paper-surface}"
    textColor: "{colors.architectural-ink}"
    rounded: "{rounded.plan}"
    padding: "12dp"
---

# Design System: Homely Android

## Overview

**Creative North Star: "Lived-in Light"**

Homely treats the floor plan as the home’s living control surface. Active devices add restrained amber illumination to their physical rooms while green controls, architectural linework, and explicit safety labels keep the interface operational rather than decorative.

The system is calm, spatial, and safety-conscious. It borrows clarity, feedback strength, and adaptive hierarchy from Apple’s design principles while preserving Android’s Material 3 controls, navigation, system Back behavior, and accessibility semantics.

**Key Characteristics:**

- Warm residential surfaces with precise architectural ink.
- The floor plan is the Home screen’s focal element.
- Live atmosphere never replaces a semantic status cue.
- Configuration remains one level deeper than immediate control.
- Compact and expanded windows preserve destination meaning.

## Colors

Warm plaster and paper neutrals carry the domestic setting; deep green owns control, amber owns live atmosphere, foliage green supports secondary context, and red is reserved for safety or failure.

### Primary

- **Deep Utility Green:** Primary actions, selected controls, and normal ON states.
- **Mint Signal Field:** Low-emphasis selected or informational containers.

### Secondary

- **Task-Light Amber:** Room illumination and active atmosphere. It is always paired with text, fill, border, or a state symbol.

### Tertiary

- **Garden Foliage:** Supporting environmental information that is neither actionable nor safety-critical.

### Neutral

- **Matte Plaster:** App background in light appearance.
- **Warm Paper:** Primary surfaces and floor-plan cards.
- **Architectural Ink:** High-emphasis text and drawn plan structure.
- **Quiet Outline:** Borders, inactive device markers, and secondary linework.
- **Night Plaster / Night Paper:** Dark-appearance background and raised surface roles.

**The Semantic Reserve Rule.** Red communicates errors and safety intervention only; amber communicates live atmosphere only.

**The Two-Cue Rule.** No device state is expressed by color alone. Pair it with a label, symbol, border treatment, or accessibility state description.

## Typography

**Display Font:** Android system sans-serif
**Body Font:** Android system sans-serif

**Character:** The type hierarchy is deliberately native and quiet. Weight and spacing create structure without competing with the floor plan or imitating a utility panel.

### Hierarchy

- **Display:** Rare, compact product-level statements only.
- **Headline:** Destination headings such as Home, Schedules, Reports, and Alerts.
- **Title:** Device names, section headings, and sheet titles.
- **Body:** Instructions, state explanations, timestamps, and recovery copy.
- **Label:** Buttons, chips, short statuses, and compact metrics.

**The Reflow Rule.** Important names, errors, and safety explanations reflow under font scaling; truncation is limited to secondary metadata.

## Layout

The system follows a 4 dp spacing rhythm and uses generous 20–24 dp destination insets. Tight controls use 8–12 dp gaps; major content groups use 18–24 dp gaps. The floor plan fills the useful Home width before summary information is introduced.

Compact windows use four peer destinations in bottom navigation. Windows at 840 dp or wider move the same ordered destinations into a navigation rail; content reorganizes instead of scaling as a single canvas. All screens respect system bars, keyboard space, RTL mirroring, and scrolling under large text.

## Elevation & Depth

Depth is primarily tonal. Material surfaces and sheets use restrained platform elevation, while the floor plan gains atmosphere through a translucent amber room glow behind active device nodes. There are no decorative glass layers, hard offset shadows, or fake physical embossing.

**The Tonal-First Rule.** Use container color and spatial separation before adding elevation; elevation exists to explain layering or selection.

## Shapes

Floor plans use a generous outer corner and a slightly tighter clipped canvas. Standard cards remain moderately rounded. Device nodes, badges, switches, and compact controls use capsule or circular geometry only where the component’s behavior earns it. Selected nodes gain an amber outline; OFF and disconnected nodes use visible neutral borders.

## Components

### Buttons

- **Shape:** Native Material capsule with a 48 dp minimum interactive height.
- **Primary:** Deep green container with white label; disabled and loading states remain explicit.
- **Focus:** Android’s native focus/pressed treatment remains intact for touch, keyboard, pointer, and accessibility input.

### Chips

- **Style:** Material filter chips select floors, devices, and schedule days.
- **State:** Selected fill is reinforced by the control’s checked semantics and stable position.

### Cards / Containers

- **Corner Style:** Moderate cards; larger floor-plan and camera containers.
- **Background:** Warm paper or semantic Material containers.
- **Shadow Strategy:** Tonal-first, with platform elevation only for genuine layer separation.
- **Internal Padding:** 14–20 dp according to density.

### Inputs / Fields

- **Style:** Material outlined fields with persistent labels and explicit HH:mm units.
- **Error / Disabled:** Invalid time values expose the standard error state; pending saves disable the primary action and name the operation.

### Navigation

- **Style:** Bottom navigation on compact windows and navigation rail on expanded windows. Home, Schedules, Reports, and Alerts remain peer destinations in that order; the alert badge carries a numeric count.

### Floor Plan and Device Marker

The custom floor plan draws a measured grid and architectural walls, then positions 48 dp device buttons from Firestore coordinates. Normal markers carry a device-type letter; error and disconnected states replace that letter with `!` or `–`. Active devices illuminate the plan without weakening the marker’s semantic state.

## Do's and Don'ts

### Do:

- **Do** make the home’s spatial state understandable before showing aggregate metrics.
- **Do** wait for Firestore-driven truth and expose pending or failed commands.
- **Do** pair every safety event with calm explanatory copy and a durable history record.
- **Do** keep common control immediate and move schedules or duration configuration one level deeper.
- **Do** preserve 48 dp targets, TalkBack names and states, dark appearance, and system navigation behavior.

### Don't:

- **Don't** turn Homely into a generic grid of equal-emphasis smart-home cards.
- **Don't** imitate Apple controls, SF Symbols, glass materials, or tab-bar styling on Android.
- **Don't** use amber as proof that a device is ON or red for ordinary emphasis.
- **Don't** add ambient looping motion, flashing, fake hardware bevels, or decorative technical typography.
- **Don't** present a client countdown as the authoritative safety mechanism.
