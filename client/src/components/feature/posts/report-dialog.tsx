import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Textarea } from '@/components/ui/textarea';
import { Label } from '@/components/ui/label';
import type { ReportReason } from '@/lib/api/schema';
import { cn } from '@/lib/utils';

const REASONS: { value: ReportReason; label: string; hint: string }[] = [
  { value: 'spam', label: 'Spam', hint: 'Ads, scams or repetitive posts' },
  { value: 'harassment', label: 'Harassment', hint: 'Targeting or bullying someone' },
  { value: 'hate', label: 'Hate', hint: 'Attacks on identity or beliefs' },
  { value: 'nudity', label: 'Nudity or sexual content', hint: '' },
  { value: 'violence', label: 'Violence or threats', hint: '' },
  { value: 'misinformation', label: 'False information', hint: '' },
  { value: 'self_harm', label: 'Self-harm', hint: 'Someone may be at risk' },
  { value: 'other', label: 'Something else', hint: '' },
];

interface ReportDialogProps {
  title?: string;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  loading?: boolean;
  onSubmit: (reason: ReportReason, details: string) => void;
}

export function ReportDialog({ title = 'Report this post', open, onOpenChange, loading, onSubmit }: ReportDialogProps) {
  const [reason, setReason] = useState<ReportReason | null>(null);
  const [details, setDetails] = useState('');

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{title}</DialogTitle>
          <DialogDescription>Reports are private. They won’t know it was you.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 px-6 pb-4">
          <fieldset>
            <legend className="sr-only">Reason</legend>
            <div className="grid gap-2">
              {REASONS.map((r) => (
                <label
                  key={r.value}
                  className={cn(
                    'flex cursor-pointer items-start gap-3 rounded-xl border p-3 transition-colors',
                    reason === r.value ? 'border-accent bg-accent-soft' : 'border-line hover:bg-surface-2',
                  )}
                >
                  <input
                    type="radio"
                    name="report-reason"
                    value={r.value}
                    checked={reason === r.value}
                    onChange={() => setReason(r.value)}
                    className="mt-1 accent-[var(--accent)]"
                  />
                  <span>
                    <span className="block text-sm font-medium">{r.label}</span>
                    {r.hint && <span className="block text-[13px] text-muted">{r.hint}</span>}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
          <div className="space-y-1.5">
            <Label htmlFor="report-details">Anything else? (optional)</Label>
            <Textarea id="report-details" maxLength={500} value={details} onChange={(e) => setDetails(e.target.value)} className="min-h-20" />
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button variant="danger" disabled={!reason} loading={loading} onClick={() => reason && onSubmit(reason, details.trim())}>
            Send report
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
