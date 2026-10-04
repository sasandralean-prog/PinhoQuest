# PinhoQuest Visual Identity Genome

> Canonical visual/UX identity for PinhoQuest V1+.
> This document governs polish work; it does not override accessibility, domain, or runtime contracts.

## 1. Emotional target

PinhoQuest should feel like entering a small, quiet place rather than opening a productivity dashboard.

Primary feelings:
- calm
- softness
- curiosity
- warmth
- gentle accomplishment
- "I can stay here for a minute"

The interface must never create urgency, guilt, FOMO, streak pressure, punitive retention, or competitive pressure.

## 2. Visual metaphor

The product metaphor is a personal garden that gradually gains life.

- Quest is the invitation to do one small thing.
- Garden is the visible memory of progress.
- Flowers are discoveries, not prizes.
- Research provides truthful botanical context.
- Artwork turns discoveries into a persistent visual keepsake.

## 3. Visual language

### Palette direction
Prefer low-saturation natural tones:
- leaf greens
- moss/olive accents
- warm cream/paper backgrounds
- soft sand
- muted lavender
- restrained terracotta
- cool blue-gray for secondary information

Avoid neon saturation, aggressive gradients, excessive pure white, and large warning-red surfaces.

### Shape language
- rounded but not inflated
- organic cards and containers
- generous whitespace
- small botanical motifs
- subtle borders over heavy shadows
- tactile garden-sign/label references where appropriate

### Typography
- friendly, rounded or humanist display face for selected headings
- highly legible system/body face for all functional text
- comfortable line height
- no fixed-height text containers
- large-font and display-scale layouts are first-class requirements

## 4. Motion

Motion should communicate life, not urgency.

Good:
- slow flower sway
- tiny leaf movement
- gentle fade/scale on discovery
- subtle garden growth
- restrained transitions between screens

Avoid:
- rapid counters
- flashing reward effects
- confetti overload
- countdowns
- attention-grabbing pulses

All motion must have a reduced-motion/accessibility-safe path.

## 5. Garden UX

Garden is the emotional center of the app.

The garden should feel like a place, not a database table.

Progressive visual composition may include:
- flowers
- small planters/beds
- leaves
- stones
- seasonal accents
- restrained ambient details

Do not turn the garden into a resource-management game unless a future approved design explicitly requires it.

## 6. Copy principles

Prefer:
- invitations
- warmth
- curiosity
- truthful uncertainty
- short sentences

Examples:
- "Que tal uma pequena aventura?"
- "Seu jardim está quietinho."
- "Uma nova flor encontrou seu jardim."

Avoid:
- "Você precisa..."
- "Não perca..."
- "Você está atrasado..."
- "Última chance..."
- manipulative reward language

## 7. Accessibility is part of identity

Soft visual design must remain readable.

Required:
- contrast validation
- large font/display scale support
- touch targets suitable for Android
- no information conveyed by color alone
- reduced-motion accommodation
- light/dark themes without losing the botanical identity

## 8. Component consistency

Every reusable component should have defined:
- semantic purpose
- typography role
- spacing
- shape/radius
- state behavior
- light/dark behavior
- accessibility behavior
- empty/loading/error behavior

A component is not considered polished merely because it looks attractive in one screenshot.

## 9. Do / Don't

### Do
- make the user breathe visually
- let whitespace exist
- use small organic details
- make progress visible without pressure
- keep interactions predictable
- preserve truthful states

### Don't
- imitate generic productivity dashboards
- overload screens with badges
- turn every action into a reward animation
- hide important state behind decorative UI
- sacrifice readability for aesthetics
- introduce visual clutter merely to make the app feel "game-like"

## 10. Governance

Visual polish must not:
- create a second source of domain truth
- bypass semantic outcomes
- introduce device-specific UI hacks
- duplicate navigation ownership
- hardcode state that belongs to Room/DataStore/domain authorities

The visual layer renders governed state; it does not invent it.