/**
 * Utilities for color name expansion
 * Converts abbreviated color names to full names in Vietnamese
 */

const COLOR_MAP = {
  // Vietnamese full names (preserve case)
  'Đen': 'Đen',
  'đen': 'Đen',
  'DEN': 'Đen',
  'Trắng': 'Trắng',
  'trắng': 'Trắng',
  'TRANG': 'Trắng',
  'Hồng': 'Hồng',
  'hồng': 'Hồng',
  'HONG': 'Hồng',
  'Đỏ': 'Đỏ',
  'đỏ': 'Đỏ',
  'DO': 'Đỏ',
  'Xanh Lam': 'Xanh Lam',
  'xanh lam': 'Xanh Lam',
  'XANHLAM': 'Xanh Lam',
  'Xanh lá': 'Xanh Lá',
  'xanh lá': 'Xanh Lá',
  'XANHLA': 'Xanh Lá',
  'Xanh la': 'Xanh Lá',
  'xanh la': 'Xanh Lá',
  'XANHLA': 'Xanh Lá',
  'Vàng': 'Vàng',
  'vàng': 'Vàng',
  'VANG': 'Vàng',
  'Tím': 'Tím',
  'tím': 'Tím',
  'TIM': 'Tím',
  'Xám': 'Xám',
  'xám': 'Xám',
  'XAM': 'Xám',
  'Nâu': 'Nâu',
  'nâu': 'Nâu',
  'NAU': 'Nâu',
  'Cam': 'Cam',
  'cam': 'Cam',
  'CAM': 'Cam',
  'Bạc': 'Bạc',
  'bạc': 'Bạc',
  'BAC': 'Bạc',
  'Kem': 'Kem',
  'kem': 'Kem',
  'KEM': 'Kem',

  // English full names
  'Black': 'Đen',
  'black': 'Đen',
  'White': 'Trắng',
  'white': 'Trắng',
  'Pink': 'Hồng',
  'pink': 'Hồng',
  'Red': 'Đỏ',
  'red': 'Đỏ',
  'Blue': 'Xanh Lam',
  'blue': 'Xanh Lam',
  'Green': 'Xanh Lá',
  'green': 'Xanh Lá',
  'Yellow': 'Vàng',
  'yellow': 'Vàng',
  'Purple': 'Tím',
  'purple': 'Tím',
  'Gray': 'Xám',
  'gray': 'Xám',
  'Grey': 'Xám',
  'grey': 'Xám',
  'Brown': 'Nâu',
  'brown': 'Nâu',
  'Orange': 'Cam',
  'orange': 'Cam',
  'Silver': 'Bạc',
  'silver': 'Bạc',
  'Gold': 'Vàng Gold',
  'gold': 'Vàng Gold',
  'Titan': 'Titan',
  'titan': 'Titan',
  'Titanium': 'Titan',
  'titanium': 'Titan',
  'Beige': 'Kem',
  'beige': 'Kem',
  'Navy': 'Navy',
  'navy': 'Navy',

  // Common uppercase abbreviations
  'BLK': 'Đen',
  'BLK.': 'Đen',
  'WHT': 'Trắng',
  'WHT.': 'Trắng',
  'PNK': 'Hồng',
  'PNK.': 'Hồng',
  'RED': 'Đỏ',
  'RED.': 'Đỏ',
  'BLU': 'Xanh Lam',
  'BLU.': 'Xanh Lam',
  'BLU.': 'Xanh Lam',
  'GRN': 'Xanh Lá',
  'GRN.': 'Xanh Lá',
  'YEL': 'Vàng',
  'YEL.': 'Vàng',
  'GLD': 'Vàng Gold',
  'GLD.': 'Vàng Gold',
  'PRP': 'Tím',
  'PRP.': 'Tím',
  'GRY': 'Xám',
  'GRY.': 'Xám',
  'SLV': 'Bạc',
  'SLV.': 'Bạc',
  'BRN': 'Nâu',
  'BRN.': 'Nâu',
  'ORG': 'Cam',
  'ORG.': 'Cam',

  // Mixed case abbreviations
  'Blk': 'Đen',
  'Blk.': 'Đen',
  'Wht': 'Trắng',
  'Wht.': 'Trắng',
  'Pnk': 'Hồng',
  'Pnk.': 'Hồng',
  'Blu': 'Xanh Lam',
  'Blu.': 'Xanh Lam',
  'Grn': 'Xanh Lá',
  'Grn.': 'Xanh Lá',
  'Yel': 'Vàng',
  'Yel.': 'Vàng',
  'Gld': 'Vàng Gold',
  'Gld.': 'Vàng Gold',
  'Prp': 'Tím',
  'Prp.': 'Tím',
  'Gry': 'Xám',
  'Gry.': 'Xám',
  'Slv': 'Bạc',
  'Slv.': 'Bạc',
  'Brn': 'Nâu',
  'Brn.': 'Nâu',
  'Org': 'Cam',
  'Org.': 'Cam',

  // Title case abbreviations
  'Blk.': 'Đen',
  'Wht.': 'Trắng',
  'Pnk.': 'Hồng',
  'Blu.': 'Xanh Lam',
  'Grn.': 'Xanh Lá',
  'Yel.': 'Vàng',
  'Gld.': 'Vàng Gold',
  'Prp.': 'Tím',
  'Gry.': 'Xám',
  'Slv.': 'Bạc',
  'Brn.': 'Nâu',
  'Org.': 'Cam',
}

/**
 * Expands abbreviated color names to full names
 * @param {string} colorName - The color name or abbreviation
 * @returns {string} The expanded color name
 */
export function expandColorName(colorName) {
  if (!colorName || typeof colorName !== 'string') return colorName || ''

  const trimmed = colorName.trim()

  // Check exact match first (case-insensitive)
  const lower = trimmed.toLowerCase()
  const upper = trimmed.toUpperCase()

  // Priority: exact match > lower case > upper case
  if (COLOR_MAP[trimmed] !== undefined) return COLOR_MAP[trimmed]
  if (COLOR_MAP[lower] !== undefined) return COLOR_MAP[lower]
  if (COLOR_MAP[upper] !== undefined) return COLOR_MAP[upper]

  // Return original if no match found
  return trimmed
}

/**
 * Expands color names in a variant string (e.g., "GLD / 128GB" -> "Vàng Gold / 128GB")
 * @param {string} variantString - The variant string containing color and other attributes
 * @returns {string} The variant string with expanded color names
 */
export function expandVariantColorNames(variantString) {
  if (!variantString || typeof variantString !== 'string') return variantString || ''

  let result = variantString

  // Replace known color abbreviations with full names
  // Sort by length (descending) to match longer abbreviations first
  const sortedKeys = Object.keys(COLOR_MAP).sort((a, b) => b.length - a.length)

  for (const key of sortedKeys) {
    // Escape special regex characters except for what we want to match
    const escapedKey = key.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    // Match whole word only (with word boundary)
    const regex = new RegExp(`\\b${escapedKey}\\b`, 'gi')
    result = result.replace(regex, COLOR_MAP[key])
  }

  return result
}
