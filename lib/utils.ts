import { type ClassValue, clsx } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatDate(date: Date | string | undefined): string {
  if (!date) return "N/A";
  const d = typeof date === "string" ? new Date(date) : date;
  return d.toLocaleDateString("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

export function formatDateTime(date: Date | string | undefined): string {
  if (!date) return "N/A";
  const d = typeof date === "string" ? new Date(date) : date;
  return d.toLocaleString("en-IN", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatName(name: string, rank: string = "", unit: string = ""): string {
  let cleaned = name.replace(/[\[\]\(\)]/g, "").replace(/\s+/g, " ").trim();
  
  const isRetd = /retd|retired/i.test(cleaned) || /retd|retired/i.test(rank) || /retired/i.test(unit);
  
  if (isRetd) {
    // Standardize retired rank or details
    // Remove "retd", "retired", and trailing rank from the name to extract the core name and initials
    let temp = cleaned;
    
    // 1. Extract and clean retired indicators
    temp = temp.replace(/\b(retd|retired)\b\.?/gi, "").trim();
    
    // 2. Extract and clean rank from name if present
    const rankRegex = /\b(DG\s*&\s*IGP|DGP|ADGP|IGP|DIGP|DIG|SP|DCP|Addl\.?\s*SP|Dy\.?\s*SP|DSP|ASP|ACP|CMDT|CPI|RPI|WPI|PI|PSI|ASI|HC|PC|AO|AAO)\b\.?/gi;
    let foundRank = "";
    const rankMatch = temp.match(rankRegex);
    if (rankMatch) {
      foundRank = rankMatch[0].toUpperCase().replace(/\s+/g, "");
      // If the found rank is DSP, map to DySP
      if (foundRank === "DSP") foundRank = "DySP";
      // If rank has no dots but equivalent has, like AddlSP vs Addl.SP
      if (foundRank === "ADDLSP") foundRank = "Addl.SP";
      temp = temp.replace(rankRegex, "").trim();
    }
    
    if (!foundRank && rank) {
      // Use the rank from the rank field
      foundRank = rank.replace(/\b(retd|retired)\b\.?/gi, "").trim();
    }
    
    // 3. Extract initials
    // Split the remaining name into words
    // Initials are words of length 1 (like "K") or length 2 ending with a dot (like "K.")
    // or concatenated single letters separated by dots (like "K.T" or "K.T.")
    const words = temp.split(" ");
    const initials: string[] = [];
    const nameWords: string[] = [];
    
    words.forEach(word => {
      const cleanWord = word.replace(/\./g, "").trim();
      // If it's a single letter, it's an initial
      if (cleanWord.length === 1 && /[a-zA-Z]/.test(cleanWord)) {
        initials.push(cleanWord.toUpperCase());
      } else if (word.includes(".")) {
        // Check if it's a combination of initials, e.g. "K.T." or "K.T"
        const subParts = word.split(".").filter(p => p.trim().length === 1);
        if (subParts.length > 0) {
          subParts.forEach(p => initials.push(p.toUpperCase()));
        } else {
          nameWords.push(word);
        }
      } else {
        nameWords.push(word);
      }
    });
    
    // 4. Format name words in Title Case
    const formattedNameWords = nameWords.map(word => {
      // Keep Dr. or Hon'ble special cases
      if (/^dr\.?$/i.test(word)) return "Dr.";
      if (/^hon'ble$/i.test(word)) return "Hon'ble";
      
      const lower = word.toLowerCase();
      return lower.charAt(0).toUpperCase() + lower.slice(1);
    });
    
    const coreName = formattedNameWords.join(" ");
    
    // Initials formatting: e.g. ["K", "T"] -> "K.T"
    let formattedInitials = initials.join(".");
    // If there is only one initial, append a dot: "K" -> "K."
    if (initials.length === 1) {
      formattedInitials += ".";
    }
    
    // Build the final string: "Name Initials Retd. Rank"
    let finalParts = [coreName];
    if (formattedInitials) {
      finalParts.push(formattedInitials);
    }
    finalParts.push("Retd.");
    if (foundRank) {
      // Normalize foundRank equivalent
      let cleanRank = foundRank.toUpperCase();
      if (cleanRank.includes("DEPUTY SUPERINTENDENT")) cleanRank = "DySP";
      else if (cleanRank.includes("SUPERINTENDENT")) cleanRank = "SP";
      else if (cleanRank.includes("INSPECTOR GENERAL")) cleanRank = "IGP";
      else if (cleanRank.includes("ADDITIONAL DIRECTOR GENERAL")) cleanRank = "ADGP";
      else if (cleanRank.includes("DIRECTOR GENERAL")) cleanRank = "DGP";
      
      finalParts.push(cleanRank);
    }
    
    return finalParts.join(" ").replace(/\s+/g, " ").trim();
  } else {
    // For non-retired officers, strip redundant rank from name if name ends with it
    let cleanName = cleaned;
    
    const uppercaseWords = new Set([
        'DG', 'IGP', 'ADGP', 'DGP', 'DIGP', 'DIG', 'SP', 'DCP', 'DySP', 'DVP', 'IG', 
        'SPL', 'SEC', 'GOVT', 'INDIA', 'CM', 'MD', 'KSPH', 'IDCL', 'BMTC', 'RERA', 
        'KAT', 'SIT', 'CID', 'IPS', 'KLA', 'ANF', 'CCT', 'SDRF', 'HRM', 'L&O'
    ]);
    
    const words = cleanName.split(' ');
    const formattedWords = words.map(word => {
        let cleanWord = word.replace(/^[^\w\&\/]+|[^\w\&\/]+$/g, '');
        let cleanUpper = cleanWord.toUpperCase();

        if (uppercaseWords.has(cleanUpper)) {
            return word.toUpperCase();
        }

        let lowered = word.toLowerCase();
        let processed = lowered.replace(/(?:^|[^a-zA-Z0-9])([a-z])/g, function(match, char) {
            return match.toUpperCase();
        });
        
        processed = processed.replace(/\bDr\b/g, 'Dr');
        return processed;
    });
    
    cleanName = formattedWords.join(' ');
    
    if (rank && cleanName.toUpperCase().endsWith(rank.toUpperCase())) {
      const stripped = cleanName.substring(0, cleanName.length - rank.length).trim();
      if (stripped.length > 2) {
        cleanName = stripped;
      }
    }
    
    return cleanName;
  }
}

/**
 * Normalize a rank string to its official KSP abbreviation.
 * Based on DIRECTORY_ORGANIZATION_RULES.md §5 "Naming Standards & Rank Abbreviations".
 *
 * Examples:
 *   "Director General of Police"          → "DGP"
 *   "Inspector General of Police"         → "IGP"
 *   "Deputy Superintendent of Police"     → "DySP"
 *   "DSP"                                 → "DySP"
 *   "Additional Superintendent of Police" → "Addl.SP"
 *   "PSI"                                 → "PSI"  (already correct, pass-through)
 */
export function normalizeRank(rank: string): string {
  if (!rank) return rank;

  const r = rank.trim();

  // Ordered from most-specific/longest to shortest to avoid partial matches
  const MAP: Array<[RegExp, string]> = [
    // DG & IGP
    [/^(director\s*general\s*(&|and)\s*inspector\s*general\s*of\s*police|dg\s*&\s*igp|dg\s*and\s*igp)$/i, "DG & IGP"],
    // DGP
    [/^(director\s*general\s*of\s*police|dgp)$/i, "DGP"],
    // ADGP
    [/^(additional\s*director\s*general\s*of\s*police|additional\s*dgp|addl\.?\s*dgp|adgp)$/i, "ADGP"],
    // IGP
    [/^(inspector\s*general\s*of\s*police|igp)$/i, "IGP"],
    // DIG / DIGP
    [/^(deputy\s*inspector\s*general\s*of\s*police|digp|dig)$/i, "DIG"],
    // Addl.SP
    [/^(additional\s*superintendent\s*of\s*police|additional\s*sp|addl\.?\s*sp|addl_sp|addlsp)$/i, "Addl.SP"],
    // SP
    [/^(superintendent\s*of\s*police|sp)$/i, "SP"],
    // DCP
    [/^(deputy\s*commissioner\s*of\s*police|dcp)$/i, "DCP"],
    // DySP / DSP / ACP
    [/^(deputy\s*superintendent\s*of\s*police|deputy\s*sp|dysp|dsp|acp|assistant\s*commissioner\s*of\s*police)$/i, "DySP"],
    // ASP
    [/^(assistant\s*superintendent\s*of\s*police|assistant\s*sp|asp)$/i, "ASP"],
    // CMDT
    [/^(commandant|cmdt)$/i, "CMDT"],
    // DEPT.CMDT
    [/^(deputy\s*commandant|dept\.?\s*cmdt|deptcmdt)$/i, "DEPT.CMDT"],
    // ASST.CMDT
    [/^(assistant\s*commandant|asst\.?\s*cmdt|asstcmdt)$/i, "ASST.CMDT"],
    // CPI
    [/^(circle\s*police\s*inspector|circle\s*pi|cpi)$/i, "CPI"],
    // RPI
    [/^(reserve\s*police\s*inspector|reserve\s*pi|rpi)$/i, "RPI"],
    // WPI
    [/^(women\s*police\s*inspector|wpi)$/i, "WPI"],
    // PI
    [/^(police\s*inspector|pi)$/i, "PI"],
    // PSI
    [/^(police\s*sub\s*inspector|sub[-\s]inspector|psi)$/i, "PSI"],
    // ASI
    [/^(assistant\s*sub\s*inspector|asi)$/i, "ASI"],
    // HC
    [/^(head\s*constable|hc)$/i, "HC"],
    // PC
    [/^(police\s*constable|pc)$/i, "PC"],
    // AO
    [/^(administrative\s*officer|ao)$/i, "AO"],
    // AAO
    [/^(assistant\s*administrative\s*officer|aao)$/i, "AAO"],
  ];

  for (const [pattern, normalized] of MAP) {
    if (pattern.test(r)) return normalized;
  }

  // No match — return as-is (already an abbreviation like "FDA", "SDA", etc.)
  return r;
}

