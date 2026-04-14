package ru.danon.spring.ToDo.services;

import java.util.List;

public interface MLClient {
    List<String> predictTags(String title, String description);

    boolean isServiceAvailable();
}
