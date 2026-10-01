package com.grupelho.discord;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

public interface ButtonHandler {

    boolean supports(String componentId);

    void handle(ButtonInteractionEvent event);
}
