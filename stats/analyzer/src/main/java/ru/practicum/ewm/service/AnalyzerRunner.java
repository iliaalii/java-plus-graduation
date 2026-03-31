package ru.practicum.ewm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.kafka.ActionProcessor;
import ru.practicum.ewm.kafka.SimilarityProcessor;

@Component
@RequiredArgsConstructor
public class AnalyzerRunner implements CommandLineRunner {
    private final ActionProcessor actionProcessor;
    private final SimilarityProcessor similarityProcessor;

    @Override
    public void run(String... args) throws Exception {
        Thread actionThread = new Thread(actionProcessor);
        actionThread.setName("UserActionProcessorThread");
        actionThread.start();

        Thread similarityThread = new Thread(similarityProcessor);
        similarityThread.setName("EventSimilarityProcessorThread");
        similarityThread.start();
    }
}
