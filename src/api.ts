import type { Config, SegmentEvent } from './types';

export const sendEvents = async ({
  config,
  events,
}: {
  config: Config;
  events: SegmentEvent[];
}) => {
  const updatedEvents = events.map((event) => {
    const updatedEvent = {
      ...event,
      sentAt: new Date().toISOString(),
    };

    // Context and Integration exist on SegmentEvents but are transmitted separately to avoid duplication
    delete updatedEvent.context;
    delete updatedEvent.integrations;

    return updatedEvent;
  });

  const body = {
    batch: updatedEvents,
    // context: events[0].context,
    // integrations: events[0].integrations,
  };
  const response = await fetch(
    `${config.proxy.scheme}://${config.proxy.host}/${config.proxy.path}`,
    {
      method: 'POST',
      body: JSON.stringify(body),
      headers: {
        'Authorization': `Bearer ${config.proxy.token}`,
        'Content-Type': 'application/json',
      },
    }
  );
  if (config.debug) {
    const result = await response.json();
    console.log(
      'Upload batch:',
      JSON.stringify(body),
      'with response: ',
      result
    );
  }
};
