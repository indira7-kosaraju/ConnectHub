import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PenSquare } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog';
import { useStartConversation } from '@/hooks/use-messages';
import { ContactPicker } from './contact-picker';

export function NewMessageDialog() {
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const start = useStartConversation();
  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="accent">
          <PenSquare aria-hidden /> New message
        </Button>
      </DialogTrigger>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New message</DialogTitle>
          <DialogDescription>You can message anyone who shares a circle with you.</DialogDescription>
        </DialogHeader>
        <div className="px-6 pb-6">
          <ContactPicker
            busyId={start.isPending ? start.variables : null}
            onPick={(u) =>
              start.mutate(u.id, {
                onSuccess: (c) => {
                  setOpen(false);
                  navigate(`/messages/${c.id}`);
                },
              })
            }
          />
        </div>
      </DialogContent>
    </Dialog>
  );
}
