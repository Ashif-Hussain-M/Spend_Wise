package com.spendwise.ui.components;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

public final class AsyncWorker {

    private AsyncWorker() {
    }

    public static <T> void execute(Callable<T> task, Consumer<T> onSuccess, Consumer<Exception> onError) {
        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    if (onError != null) {
                        onError.accept((Exception) cause);
                    }
                }
            }
        };
        worker.execute();
    }

    public static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
