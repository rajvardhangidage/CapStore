---
version: alpha
name: 52degree
description: High-contrast, fashion-forward e-commerce system with a bold editorial voice and minimal chrome.
colors:
  primary: "#1f38d3"
  secondary: "#282828"
  tertiary: "#e5e7eb"
  neutral: "#ffffff"
  surface: "#ffffff"
  on-surface: "#282828"
  error: "#d32f2f"
  primary-strong: "#1230c9"
  primary-soft: "#eef2ff"
  border: "#e5e7eb"
typography:
  headline-display:
    fontFamily: "ClashDisplay"
    fontSize: "64px"
    fontWeight: 700
    lineHeight: "77px"
    letterSpacing: "0px"
  headline-lg:
    fontFamily: "ClashDisplay"
    fontSize: "45px"
    fontWeight: 500
    lineHeight: "54px"
    letterSpacing: "-0.7px"
  headline-md:
    fontFamily: "ClashDisplay"
    fontSize: "32px"
    fontWeight: 500
    lineHeight: "54px"
    letterSpacing: "-0.9px"
  headline-sm:
    fontFamily: "ClashDisplay"
    fontSize: "23px"
    fontWeight: 500
    lineHeight: "28px"
    letterSpacing: "0px"
  body-lg:
    fontFamily: "ClashDisplay"
    fontSize: "18px"
    fontWeight: 400
    lineHeight: "28px"
    letterSpacing: "0px"
  body-md:
    fontFamily: "ClashDisplay"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: "24px"
    letterSpacing: "0px"
  body-sm:
    fontFamily: "ClashDisplay"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: "20px"
    letterSpacing: "0px"
  label-lg:
    fontFamily: "ClashDisplay"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: "24px"
    letterSpacing: "0px"
  label-md:
    fontFamily: "ClashDisplay"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: "20px"
    letterSpacing: "0px"
  label-sm:
    fontFamily: "ClashDisplay"
    fontSize: "12px"
    fontWeight: 500
    lineHeight: "16px"
    letterSpacing: "0.02em"
  overline:
    fontFamily: "ClashDisplay"
    fontSize: "12px"
    fontWeight: 600
    lineHeight: "16px"
    letterSpacing: "0.08em"
rounded:
  none: 0px
  sm: 4px
  md: 8px
  lg: 12px
  xl: 16px
  full: 9999px
spacing:
  xs: 10px
  sm: 20px
  md: 30px
  lg: 40px
  xl: 152px
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    padding: "15px 35px"
    size: "168px"
    height: "46px"
  button-secondary:
    backgroundColor: "{colors.neutral}"
    textColor: "{colors.on-surface}"
    typography: "{typography.label-lg}"
    rounded: "{rounded.sm}"
    padding: "15px 35px"
    size: "168px"
    height: "46px"
  button-tertiary:
    backgroundColor: "transparent"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.none}"
    padding: "0px"
    size: "auto"
    height: "auto"
  card:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    rounded: "{rounded.md}"
    padding: "16px"
    size: "auto"
  input:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.on-surface}"
    typography: "{typography.body-md}"
    rounded: "{rounded.sm}"
    padding: "12px 16px"
    height: "46px"
  chip:
    backgroundColor: "{colors.primary-soft}"
    textColor: "{colors.primary}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "6px 12px"
  badge:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.neutral}"
    typography: "{typography.label-sm}"
    rounded: "{rounded.full}"
    padding: "4px 8px"
# 52degree

## Overview

52degree feels like a premium streetwear and lifestyle brand with a strong editorial edge. The interface is sparse and confident: lots of white space, one vivid blue accent, and oversized typography that leads with attitude rather than decoration. It targets shoppers who respond to bold fashion messaging, promotional urgency, and a clean, modern storefront experience.

## Colors

- **Primary (#1f38d3):** A saturated electric blue used for the logo, utility actions, and key brand moments. It is the system’s signature accent and should signal interactivity, emphasis, and brand recognition.
- **Secondary / On-surface (#282828):** A deep charcoal used for top-bar text, body copy, icons, and outlines. It provides strong readability without the harshness of pure black.
- **Neutral / Surface (#ffffff):** Clean white used as the default page background and card surface, keeping the layout bright, airy, and highly legible.
- **Tertiary / Border (#e5e7eb):** A soft cool gray used for subtle dividers, card borders, and low-emphasis framing. It keeps structure visible without adding visual weight.
- **Primary Strong (#1230c9):** A deeper blue variant for hover or pressed states when a stronger accent is needed.
- **Primary Soft (#eef2ff):** A pale blue tint for chips, soft highlights, and low-emphasis contextual surfaces.
- **Error (#d32f2f):** Reserved for validation and destructive states, and should stay visually separate from the core blue brand language.

## Typography

The system is built on **ClashDisplay**, which gives the brand its tall, condensed, fashion-editorial voice. Headlines use strong sizes and tight tracking to create dramatic hierarchy, while body text stays simple and readable in the same family for consistency.

- **Headline display, large, and medium styles** are used for hero statements, campaign slogans, and promotional panels. The larger sizes should feel loud, vertical, and poster-like.
- **Headline small** is suitable for section titles, navigation-support copy, and compact marketing headings.
- **Body styles** are restrained and functional, supporting product details, announcements, and general UI copy without competing with the hero treatment.
- **Label styles** should be used for buttons, utility navigation, badges, and microcopy. The `overline` token can support all-caps or highly emphasized editorial labels with wider letter spacing.
- Uppercase usage is prominent in the source, especially for marketing headlines. Keep letter spacing slightly expanded for small caps or all-caps treatments, but avoid over-spacing body copy.

## Layout

The layout is spacious and centered, with a strong preference for broad horizontal breathing room and simple alignment over dense multi-column complexity. The hero is full-bleed and visually dominant, while the top navigation stays compact and centered within a wide container.

Spacing should follow a clear rhythm based on the extracted scale: `xs` 10px, `sm` 20px, `md` 30px, `lg` 40px, and `xl` 152px. Use tighter values for utility rows, medium spacing for component stacks, and large spacing for section separation and hero framing.

Cards and content blocks should use comfortable internal padding, typically 16px or more, with clean edges and minimal nested structure. The overall feel should remain uncluttered, with generous negative space preferred over dense grouping.

## Elevation & Depth

The system is mostly flat. Instead of heavy shadows, it relies on contrast, typography scale, and image-led composition to create hierarchy. Borders are subtle and surfaces stay white, so depth comes from layering text over imagery and using tonal separation sparingly.

When shadow is needed, keep it minimal and soft, matching the existing low-elevation style rather than introducing dramatic floating panels. Strong visual contrast between the blue accent, white surfaces, and charcoal text should remain the primary organizing tool.

## Shapes

The shape language is restrained and lightly rounded. Buttons use a small 4px radius, cards use a modest 8px radius, and pills/badges can round fully for a softer utility feel.

Overall, the system reads as clean and contemporary rather than playful. Corners should feel precise and controlled, with no exaggerated curves except where a pill shape is intentionally needed.

## Components

### Buttons
- `button-primary` is the main call to action: solid blue, white text, 46px tall, and padded generously for a premium tap target.
- `button-secondary` uses a white background with a charcoal border and text, keeping attention lower than the primary action while still reading as clickable.
- `button-tertiary` should be text-only or minimally styled, used for secondary navigation links and subtle actions.
- Keep button typography medium-weight and compact. Avoid oversized pill buttons; the existing language prefers rectangular forms with a slight radius.
- Hover states should deepen the blue slightly for primary actions and maintain clear legibility for secondary actions.

### Navigation
- The top navigation should stay understated and centered, with blue text links and plenty of horizontal spacing.
- Icons should be minimal and monochrome, supporting search, account, and cart actions without extra decoration.

### Cards
- `card` should remain white with a light border, modest radius, and no heavy shadow.
- Use cards for product tiles, content previews, and promotional modules, but keep content density low and padding consistent.
- Borders are more important than elevation for separating cards from the page.

### Inputs
- Inputs should mirror the button and card geometry: 46px tall, subtle radius, white surface, and charcoal text.
- Use clear border treatment and avoid heavy fills; focus on clarity and straightforward affordance.
- Placeholder text should be subdued, but still readable against the white background.

### Chips and Badges
- `chip` should use a soft blue tint with blue text for lightweight categorization, filters, or selectable tags.
- `badge` should use solid blue with white text for counts, notifications, and small urgency markers.
- Keep these compact and highly legible; they should support the brand accent without competing with hero content.

## Do's and Don'ts

- Do use the primary blue sparingly but consistently for brand-critical actions and recognition.
- Do preserve the spacious, editorial feel with generous margins and clear visual hierarchy.
- Do use ClashDisplay for all UI text to maintain a unified fashion-forward tone.
- Do keep surfaces white and borders subtle so imagery and typography remain the focus.
- Don't introduce dark themes, gradient-heavy surfaces, or ornamental shadows.
- Don't use rounded, bubbly, or playful shapes that clash with the sharp editorial identity.
- Don't overcrowd pages with too many equal-weight components; let hero content dominate.
- Don't replace the condensed typographic voice with generic sans-serif system fonts.