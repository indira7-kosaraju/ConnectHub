import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { toast } from '@/components/ui/toaster';
import { messagesApi } from '@/lib/api/messages';
import { ApiRequestError } from '@/lib/api/client';
import type { PostDto } from '@/lib/api/schema';
import { qk } from '@/lib/query-keys';
import { realtime } from '@/lib/realtime';
import { ContactPicker } from './contact-picker';

/** Send a post into a 1:1 conversation, with an optional note. */
export function SharePostDialog({ post, open, onOpenChange }: { post: PostDto; open: boolean; onOpenChange: (o: boolean) => void }) {
  const qc = useQueryClient();
  const [note, setNote] = useState('');
  const [busy, setBusy] = useState<string | null>(null);

  const share = async (userId: string, name: string) => {
    setBusy(userId);
    try {
      const convo = await messagesApi.start(userId);
      const frame = { conversationId: convo.id, content: note.trim() || undefined, sharedPostId: post.id, clientId: crypto.randomUUID() };
      if (!realtime.publish('/app/chat.send', frame)) await messagesApi.send(convo.id, frame);
      void qc.invalidateQueries({ queryKey: qk.conversations });
      toast.success(`Sent to ${name}`);
      setNote('');
      onOpenChange(false);
    } catch (err) {
      toast.error(err instanceof ApiRequestError ? err.message : 'Could not send');
    } finally {
      setBusy(null);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Send in a message</DialogTitle>
          <DialogDescription>
            {post.circle.privateCircle ? 'This post is from a private circle — only members of it will be able to open it.' : 'Share this post with someone from your circles.'}
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 px-6 pb-6">
          <Input value={note} onChange={(e) => setNote(e.target.value)} maxLength={2000} placeholder="Add a note (optional)" aria-label="Note" />
          <ContactPicker busyId={busy} onPick={(u) => void share(u.id, u.displayName ?? u.username)} />
        </div>
      </DialogContent>
    </Dialog>
  );
}
