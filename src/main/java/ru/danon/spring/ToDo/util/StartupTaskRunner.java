package ru.danon.spring.ToDo.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.danon.spring.ToDo.services.TaskService;

@Component
public class StartupTaskRunner implements ApplicationRunner {
    private final TaskService taskService;

    @Autowired
    public StartupTaskRunner(TaskService taskService) {
        this.taskService = taskService;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("Запуск проверки просроченных задач при старте...");
        taskService.updateOverdueTasks();
        System.out.println("Проверка завершена.");
    }
}
