package com.eagleeye.ui.theme

import androidx.compose.ui.graphics.Color

// EagleEye palette — refined dark, inspired by Linear/Vercel/GitHub dashboards.
// The "Cyber*" names are kept for codebase stability; values are softened from
// the original neon palette to read as a polished security tool rather than
// a 1990s terminal.

// Semantic accents (used for severity and state, not for branding)
val CyberGreen     = Color(0xFF10B981)  // emerald-500 — connected / safe
val CyberGreenDim  = Color(0xFF059669)  // emerald-600 — darker variant
val CyberBlue      = Color(0xFF3B82F6)  // blue-500 — info / primary actions
val CyberRed       = Color(0xFFEF4444)  // red-500 — critical / threats
val CyberOrange    = Color(0xFFF59E0B)  // amber-500 — warning
val CyberYellow    = Color(0xFFFACC15)  // yellow-400 — caution / weak security

// Surface hierarchy — near-black with subtle warmth, layered tones
val BackgroundDark     = Color(0xFF0A0A0B)  // canvas
val SurfaceDark        = Color(0xFF111113)  // tab bar, top bar
val SurfaceVariantDark = Color(0xFF1A1A1D)  // raised surfaces (dropdowns)
val CardDark           = Color(0xFF141417)  // cards
val CardBorderDark     = Color(0xFF26262B)  // subtle borders

// Text hierarchy via opacity-style values
val TextPrimary   = Color(0xFFF5F5F7)  // primary copy
val TextSecondary = Color(0xFFA1A1AA)  // secondary / metadata
val TextDim       = Color(0xFF71717A)  // hints / placeholders / disabled
