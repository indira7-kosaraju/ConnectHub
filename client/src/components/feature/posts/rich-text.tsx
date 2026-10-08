import { Fragment } from 'react';

const URL_RE = /(https?:\/\/[^\s<]+[^\s<.,:;"')\]!?])/g;

/** Plain text with line breaks preserved and http(s) links made clickable. Never renders HTML. */
export function RichText({ text, className }: { text: string; className?: string }) {
  const parts = text.split(URL_RE);
  return (
    <p className={className}>
      {parts.map((part, i) =>
        i % 2 === 1 ? (
          <a key={i} href={part} target="_blank" rel="noopener noreferrer nofollow ugc" className="break-all text-accent-ink underline-offset-2 hover:underline">
            {part.replace(/^https?:\/\//, '')}
          </a>
        ) : (
          <Fragment key={i}>{part}</Fragment>
        ),
      )}
    </p>
  );
}
