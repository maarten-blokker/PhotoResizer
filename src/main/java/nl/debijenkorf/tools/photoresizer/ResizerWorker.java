package nl.debijenkorf.tools.photoresizer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javafx.scene.paint.Color;
import nl.debijenkorf.tools.photoresizer.resizer.ImageResizerService;
import nl.debijenkorf.tools.photoresizer.resizer.Preset;
import nl.debijenkorf.tools.photoresizer.resizer.impl.ImgScalrResizer;
import nl.debijenkorf.tools.photoresizer.resizer.impl.SmartImageAligner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Maarten Blokker
 */
public class ResizerWorker {

    private static final Logger LOG = LoggerFactory.getLogger(ResizerWorker.class);

    private static final AtomicLong THREAD_COUNTER = new AtomicLong();
    private static final String IMAGE_FILE_PATTERN = "glob:**/*.{jpg,jpeg,png,bmp,gif,tif,tiff}";

    private final ImageResizerService resizer = new ImgScalrResizer(new SmartImageAligner());
    private final AtomicBoolean running = new AtomicBoolean();
    private final ListeningExecutorService service;
    private final Preset preset;
    private final Color color;
    private final Path srcDir;
    private final Path dstDir;

    private Listener listener;
    private Throwable exception;
    private boolean finished;

    public ResizerWorker(Preset preset, Color color, Path srcDir, Path dstDir) {
        this.preset = preset;
        this.color = color;
        this.srcDir = srcDir;
        this.dstDir = dstDir;

        int cores = Runtime.getRuntime().availableProcessors();
        int threads = Math.max(2, cores * 2 - 1);
        service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(threads, (r) -> {
            Thread t = new Thread(r);
            t.setName("resize-worker-" + THREAD_COUNTER.getAndIncrement());
            t.setDaemon(false);

            return t;
        }));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isFinished() {
        return finished;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            LOG.info("Starting processing of files");
            Futures.addCallback(findFiles(), handleResult((files) -> {
                LOG.info("Found {} files to process", files.size());
                List<ListenableFuture<?>> futures = files.stream().map(this::processFile).collect(Collectors.toList());
                AtomicInteger current = new AtomicInteger();

                futures.forEach((future) -> {
                    future.addListener(() -> {
                        setProgress(current.incrementAndGet() / (double) files.size());
                    }, MoreExecutors.directExecutor());
                });

                Futures.addCallback(Futures.allAsList(futures), handleResult((result) -> {
                    finish(true);
                }));
            }));
        } else {
            throw new IllegalStateException("Resizer is allready running");
        }
    }

    public void stop() {
        finish(true);
        service.shutdownNow();
    }

    private ListenableFuture<List<Path>> findFiles() {
        return service.submit(() -> {
            try {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher(IMAGE_FILE_PATTERN);

                return Files.list(srcDir)
                        .filter(matcher::matches)
                        .collect(Collectors.toList());
            } catch (IOException ex) {
                throw new IllegalStateException("Failed to collect files from directory", ex);
            }
        });
    }

    private ListenableFuture<?> processFile(Path srcFile) {
        String filename = srcFile.getFileName().toString();
        int index = filename.indexOf('.');
        if (index > 0) {
            filename = filename.substring(0, index);
        }

        Path dstFile = dstDir.resolve(filename + ".jpg");

        java.awt.Color awtColor = new java.awt.Color(
                (float) color.getRed(),
                (float) color.getGreen(),
                (float) color.getBlue(),
                (float) color.getOpacity()
        );

        return service.submit(() -> {
            try (InputStream in = Files.newInputStream(srcFile);
                    OutputStream out = Files.newOutputStream(dstFile, StandardOpenOption.CREATE)) {
                resizer.process(preset, awtColor, true, in, out);
                LOG.info("Processed file: {}", dstFile);
            } catch (IOException ex) {
                deleteQuitly(dstFile);
                throw new IllegalStateException("Failed to process file: " + srcFile, ex);
            }
        });
    }

    private void setProgress(double progress) {
        if (listener != null) {
            listener.onProgress(this, progress);
        }
    }

    private void exception(Throwable t) {
        LOG.error("Processing stopped due to exception", t);
        service.shutdown();
        exception = t;
        finish(false);
    }

    private void finish(boolean succesfull) {
        LOG.info("Processing finished, succesfull={}", succesfull);
        running.set(false);
        finished = true;
        if (listener != null) {
            listener.onProgress(this, 1D);
            listener.onFinish(this, succesfull);
        }
    }

    private void deleteQuitly(Path file) {
        if (Files.exists(file)) {
            try {
                Files.delete(file);
            } catch (IOException ex) {
                LOG.error("Failed to delete file on exception");
            }
        }
    }

    private <T> FutureCallback<T> handleResult(Consumer<T> handler) {
        return new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                handler.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
                exception(t);
            }
        };
    }

    public static interface Listener {

        void onProgress(ResizerWorker worker, double progress);

        void onFinish(ResizerWorker worker, boolean succesfull);
    }

}
