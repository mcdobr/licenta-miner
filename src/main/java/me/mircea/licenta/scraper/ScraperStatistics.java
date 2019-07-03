package me.mircea.licenta.scraper;

import com.google.common.base.Stopwatch;
import me.mircea.licenta.core.crawl.db.model.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple utility class to log time statistics for the scraper.
 */

class ScraperStatistics {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScraperStatistics.class);
    private static final int STATISTICS_BATCH_SIZE = 10_000;

    private final Job job;
    private AtomicInteger noPagesToBeRequested;
    private AtomicInteger noRequests;
    private AtomicInteger noPagesReached;
    private AtomicInteger noProductOfferPairsFound;

    private Duration totalDuration;
    private Duration totalDownloadDuration;
    private Duration totalProcessingDuration;
    private Duration totalCrawlPersistenceDuration;
    private Duration totalProductPersistenceDuration;

    private Stopwatch totalDurationTimer;
    private Stopwatch downloadDurationTimer;
    private Stopwatch processingDurationTimer;
    private Stopwatch crawlPersistenceDurationTimer;
    private Stopwatch productPersistenceDurationTimer;

    ScraperStatistics(Job job) {
        this.job = job;
        this.noPagesToBeRequested = new AtomicInteger(0);
        this.noRequests = new AtomicInteger(0);
        this.noPagesReached = new AtomicInteger(0);
        this.noProductOfferPairsFound = new AtomicInteger(0);

        this.totalDuration = Duration.ZERO;
        this.totalDownloadDuration = Duration.ZERO;
        this.totalProcessingDuration = Duration.ZERO;
        this.totalCrawlPersistenceDuration = Duration.ZERO;
        this.totalProductPersistenceDuration = Duration.ZERO;
    }


    void incrementNumberOfPagesToBeRequested() {
        noPagesToBeRequested.getAndIncrement();
    }

    void incrementNumberOfRequests() {
        noRequests.getAndIncrement();
    }

    void incrementNumberOfPagesReached() {
        noPagesReached.getAndIncrement();
    }

    void incrementNumberOfPagesProductOfferPairsFound() {
        noProductOfferPairsFound.getAndIncrement();
    }

    boolean isBatchReady() {
        return noPagesToBeRequested.get() % STATISTICS_BATCH_SIZE == 0;
    }

    void startTotalDurationTimer() {
        totalDurationTimer = Stopwatch.createStarted();
    }

    void stopTotalDurationTimer() {
        totalDurationTimer.stop();
        totalDuration = totalDurationTimer.elapsed();
    }

    void startDownloadDurationTimer() {
        downloadDurationTimer = Stopwatch.createStarted();
    }

    void stopDownloadDurationTimer() {
        downloadDurationTimer.stop();
        totalDownloadDuration = totalDownloadDuration.plus(downloadDurationTimer.elapsed());
    }

    void startProcessingDurationTimer() {
        processingDurationTimer = Stopwatch.createStarted();
    }

    void stopProcessingDurationTimer() {
        processingDurationTimer.stop();
        totalProcessingDuration = totalProcessingDuration.plus(processingDurationTimer.elapsed());
    }

    void startCrawlPersistenceDurationTimer() {
        crawlPersistenceDurationTimer = Stopwatch.createStarted();
    }

    void stopCrawlPersistenceDurationTimer() {
        crawlPersistenceDurationTimer.stop();
        totalCrawlPersistenceDuration = totalCrawlPersistenceDuration.plus(crawlPersistenceDurationTimer.elapsed());
    }

    void startProductPersistenceDurationTimer() {
        productPersistenceDurationTimer = Stopwatch.createStarted();
    }

    void stopProductPersistenceDurationTimer() {
        productPersistenceDurationTimer.stop();
        totalProductPersistenceDuration = totalProcessingDuration.plus(productPersistenceDurationTimer.elapsed());
    }

    void logStatistics() {
        LOGGER.info("Domain {}, number of pages to be requested: {}", job.getDomain(), noPagesToBeRequested);
        LOGGER.info("Domain {}, number of GET requests made: {}", job.getDomain(), noRequests);
        LOGGER.info("Domain {}, number of pages reached: {}", job.getDomain(), noPagesReached);
        LOGGER.info("Domain {}, number of product-offer pairs found: {}", job.getDomain(), noProductOfferPairsFound);


        LOGGER.info("Domain {}, total duration: {}", job.getDomain(), totalDuration);
        LOGGER.info("Domain {}, total download of page time: {}", job.getDomain(), totalDownloadDuration);
        LOGGER.info("Domain {}, total processing of page time: {}", job.getDomain(), totalProcessingDuration);
        LOGGER.info("Domain {}, total crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration);
        LOGGER.info("Domain {}, total product persistence of product time: {}", job.getDomain(), totalProductPersistenceDuration);


        if (noPagesToBeRequested.get() != 0) {
            LOGGER.info("Domain {}, average download of page time: {}", job.getDomain(), totalDownloadDuration.dividedBy(noPagesToBeRequested.get()));
        }
        if (noPagesToBeRequested.get() != 0) {
            LOGGER.info("Domain {}, average processing of page time: {}", job.getDomain(), totalProcessingDuration.dividedBy(noPagesReached.get()));
        }
        if (noPagesReached.get() != 0) {
            LOGGER.info("Domain {}, average crawl persistence of product time: {}", job.getDomain(), totalCrawlPersistenceDuration.dividedBy((noPagesReached.get())));
        }
        if (noProductOfferPairsFound.get() != 0) {
            LOGGER.info("Domain {}, average product persistence of product time: {}", job.getDomain(), totalProductPersistenceDuration.dividedBy(noProductOfferPairsFound.get()));
        }
    }
}
