package com.bookstore.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "bookstore.avatar")
public class AvatarProperties {

    private List<String> presets = List.of(
        "avatars/default/avatar1.png",
        "avatars/default/avatar2.png",
        "avatars/default/avatar3.png",
        "avatars/default/avatar4.png",
        "avatars/default/avatar5.png",
        "avatars/default/avatar6.png"
    );
}
