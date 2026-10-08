import { useParams } from 'react-router-dom';
import { PageHeader } from '@/components/ui/page-header';
import { ConversationList } from '@/components/feature/messages/conversation-list';
import { NewMessageDialog } from '@/components/feature/messages/new-message-dialog';
import { ChatThread } from '@/components/feature/messages/chat-thread';

export function MessagesPage() {
  return (
    <>
      <PageHeader title="Messages" description="One-to-one with people from your circles." actions={<NewMessageDialog />} />
      <ConversationList />
    </>
  );
}

export function ConversationPage() {
  const { id = '' } = useParams<{ id: string }>();
  return <ChatThread key={id} conversationId={id} />;
}
