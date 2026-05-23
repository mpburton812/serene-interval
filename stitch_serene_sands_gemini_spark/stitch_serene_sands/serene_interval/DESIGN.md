---
name: Serene Interval
colors:
  surface: '#fbf9f4'
  surface-dim: '#dbdad5'
  surface-bright: '#fbf9f4'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f5f3ee'
  surface-container: '#f0eee9'
  surface-container-high: '#eae8e3'
  surface-container-highest: '#e4e2dd'
  on-surface: '#1b1c19'
  on-surface-variant: '#424842'
  inverse-surface: '#30312e'
  inverse-on-surface: '#f2f1ec'
  outline: '#737972'
  outline-variant: '#c2c8c0'
  surface-tint: '#4a654e'
  primary: '#4a654e'
  on-primary: '#ffffff'
  primary-container: '#8ba88e'
  on-primary-container: '#233d29'
  inverse-primary: '#b0ceb2'
  secondary: '#496172'
  on-secondary: '#ffffff'
  secondary-container: '#c9e3f7'
  on-secondary-container: '#4d6677'
  tertiary: '#8d4d3b'
  on-tertiary: '#ffffff'
  tertiary-container: '#da8d78'
  on-tertiary-container: '#5d2718'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cceace'
  primary-fixed-dim: '#b0ceb2'
  on-primary-fixed: '#07200f'
  on-primary-fixed-variant: '#334d38'
  secondary-fixed: '#cce6fa'
  secondary-fixed-dim: '#b0cadd'
  on-secondary-fixed: '#021e2c'
  on-secondary-fixed-variant: '#314a5a'
  tertiary-fixed: '#ffdbd1'
  tertiary-fixed-dim: '#ffb5a1'
  on-tertiary-fixed: '#380c02'
  on-tertiary-fixed-variant: '#703626'
  background: '#fbf9f4'
  on-background: '#1b1c19'
  surface-variant: '#e4e2dd'
typography:
  headline-xl:
    fontFamily: Plus Jakarta Sans
    fontSize: 40px
    fontWeight: '600'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 40px
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 28px
    fontWeight: '500'
    lineHeight: 34px
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-margin: 24px
  gutter: 16px
  stack-sm: 8px
  stack-md: 16px
  stack-lg: 32px
  stack-xl: 64px
---

## Brand & Style

The brand personality is grounded in tranquility, mindfulness, and digital restoration. Designed for individuals seeking a sanctuary from the high-velocity demands of modern life, the interface acts as a visual deep breath.

The design style is a hybrid of **Minimalism** and **Soft Glassmorphism**. By prioritizing heavy whitespace and a reductionist approach to UI chrome, the focus remains entirely on the meditative content. The aesthetic avoids the sterile coldness of traditional minimalism by introducing organic textures, soft tonal transitions, and translucent layers that suggest depth without creating cognitive load. The emotional response should be one of immediate relief, quietude, and effortless flow.

## Colors

The palette is derived from natural, desaturated elements—water, flora, and earth. To maintain a peaceful environment, high-contrast black is strictly avoided; instead, a deep charcoal-green is used for text to ensure legibility while remaining soft on the eyes.

- **Primary (Sage):** Used for growth-oriented actions, progress indicators, and active states.
- **Secondary (Soft Blue):** Applied to atmospheric elements, backgrounds, and evening-focused meditations.
- **Tertiary (Terracotta):** Reserved for delicate accents, "heart" interactions, or sun-rise sessions.
- **Neutral (Cream):** The canvas color. It provides a warmer, more human foundation than pure white.
- **Surface (Translucent):** A semi-transparent white (Alpha 40-60%) used for glassmorphic containers.

## Typography

The typographic system balances the modern, approachable curves of **Plus Jakarta Sans** for headings with the systematic clarity of **Inter** for long-form reading and interface labels. 

The hierarchy is intentionally sparse. Large headlines use generous leading to prevent a cramped feeling. Body text is set with slightly increased line height (1.5x+) to enhance readability and mental ease. Letter spacing is slightly tightened on large headings for a contemporary look, while labels are tracked out to provide a sense of breathability in tight spaces.

## Layout & Spacing

This design system utilizes a **fluid-to-fixed** hybrid model. For mobile, a single-column layout with 24px side margins ensures content feels centered and important. For larger displays, the content is capped at a 1200px container to prevent eye strain.

Spacing follows an 8px base grid, but the "mental spacing" is the priority. Negative space should always feel slightly "too large" rather than "just right." Use `stack-xl` to separate major content sections, allowing the user's eyes to rest as they scroll. Avoid dense grids; elements should feel like they are floating in an open, airy environment.

## Elevation & Depth

Depth is communicated through **Glassmorphism** and **Ambient Shadows**. Instead of traditional shadows that imply weight, this system uses "luminous" depth.

1.  **Base Layer:** The warm cream neutral background.
2.  **Glass Layer:** Semi-transparent panels with a 20px - 40px backdrop blur. These layers should have a 1px solid white border at 20% opacity to define the edge.
3.  **Soft Elevation:** For floating elements (like play buttons), use a very large blur radius (30px+) with a low-opacity tint derived from the primary color (Sage) rather than gray. This creates a soft glow effect rather than a harsh drop shadow.

## Shapes

The shape language is organic and soft. There are no sharp corners in the design system. 

- **Standard Containers:** Use 0.5rem (8px) for small utility items.
- **Main Cards & Glass Panels:** Use 1rem (16px) to create a friendly, approachable container.
- **Interactive Elements:** Buttons and tags utilize a fully rounded (Pill) style to encourage touch and interaction.

## Components

- **Buttons:** Primary buttons are pill-shaped, using the Sage green with white text. Secondary buttons use a glassmorphic background with a Sage border.
- **Glass Cards:** Used for meditation sessions. They feature a high backdrop blur, subtle white inner stroke, and a very soft, tinted shadow.
- **Chips:** Small, pill-shaped tags for "Minutes," "Focus," or "Topic." Use low-saturation background tints (e.g., 10% opacity Sage) with full-saturation text.
- **Input Fields:** Minimalist underlines or soft-filled containers with rounded corners (8px). Focus states should transition the border color to Soft Blue with a subtle outer glow.
- **Progress Bars:** Thin, organic lines. The "track" is a low-opacity version of the "fill" color. No sharp edges on the bar ends.
- **Navigation:** A bottom bar using a heavy backdrop-blur (Glassmorphism) to allow the background colors to bleed through, making the UI feel integrated with the environment.