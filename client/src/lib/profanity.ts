/**
 * Client copy of the server's soft profanity list (server/src/main/resources/profanity.txt), used
 * only to warn before posting. The server masks words regardless; this just avoids surprises.
 */
const WORDS = [
  'motherfuck', 'fuck', 'bullshit', 'shit', 'bitch', 'asshole', 'arsehole', 'bastard', 'cunt', 'dickhead', 'dick',
  'pussy', 'cock', 'prick', 'twat', 'wanker', 'slut', 'whore', 'douche', 'jackass', 'faggot', 'fag', 'nigger',
  'nigga', 'retard', 'spastic',
];
const LEET: Record<string, string> = { a: '[a@4]', e: '[e3]', i: '[i1!|]', o: '[o0]', s: '[s5$]', t: '[t7+]', g: '[g9]', l: '[l1|]' };
const escape = (c: string) => c.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const PATTERN = new RegExp(
  `(?<![\\p{L}\\p{N}])(?:${WORDS.map((w) => [...w].map((c) => `${LEET[c] ?? escape(c)}+`).join('')).join('|')})(?:s|es|ed|er|ers|ing|in|y|head|heads)?(?![\\p{L}\\p{N}])`,
  'iu',
);

export function hasProfanity(text: string): boolean {
  return PATTERN.test(text);
}
