import { useState } from 'react';
import { Avatar } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useAuthStore } from '@/stores/auth-store';
import { ProfanityHint } from '@/components/ui/profanity-hint';

interface CommentFormProps {
  onSubmit: (content: string) => Promise<unknown>;
  placeholder?: string;
  autoFocus?: boolean;
  onCancel?: () => void;
  compact?: boolean;
}

export function CommentForm({ onSubmit, placeholder = 'Add a comment…', autoFocus, onCancel, compact }: CommentFormProps) {
  const user = useAuthStore((s) => s.user);
  const [text, setText] = useState('');
  const [busy, setBusy] = useState(false);

  const submit = async () => {
    const content = text.trim();
    if (!content || busy) return;
    setBusy(true);
    try {
      await onSubmit(content);
      setText('');
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      className="flex items-start gap-3"
      onSubmit={(e) => {
        e.preventDefault();
        void submit();
      }}
    >
      <Avatar size={compact ? 'sm' : 'md'} src={user?.avatarUrl} name={user?.displayName ?? user?.username} />
      <div className="flex-1">
        <Textarea
          aria-label={placeholder}
          placeholder={placeholder}
          autoFocus={autoFocus}
          maxLength={500}
          rows={1}
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && (e.metaKey || e.ctrlKey)) void submit();
            if (e.key === 'Escape') onCancel?.();
          }}
          className="min-h-11 py-2.5"
        />
        <ProfanityHint text={text} className="mt-1 text-xs text-accent-ink" />
        {(text.length > 0 || onCancel) && (
          <div className="mt-2 flex justify-end gap-2">
            {onCancel && (
              <Button type="button" size="sm" variant="ghost" onClick={onCancel}>
                Cancel
              </Button>
            )}
            <Button type="submit" size="sm" loading={busy} disabled={!text.trim()}>
              {compact ? 'Reply' : 'Comment'}
            </Button>
          </div>
        )}
      </div>
    </form>
  );
}
