/**
 * 
 */
package lib.structure.capr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.milaboratory.core.PairedEndReadsLayout;
import com.milaboratory.core.alignment.AffineGapAlignmentScoring;
import com.milaboratory.core.alignment.Aligner;
import com.milaboratory.core.alignment.Alignment;
import com.milaboratory.core.io.sequence.PairedRead;
import com.milaboratory.core.io.sequence.SingleReadImpl;
import com.milaboratory.core.merger.MismatchOnlyPairedReadMerger;
import com.milaboratory.core.merger.PairedReadMergingResult;
import com.milaboratory.core.merger.QualityMergingAlgorithm;
import com.milaboratory.core.sequence.NSequenceWithQuality;
import com.milaboratory.core.sequence.NucleotideSequence;

import exceptions.InvalidConfigurationException;
import lib.aptamer.datastructures.SelectionCycle;
import lib.parser.aptaplex.distances.BitapDistance;
import lib.parser.aptaplex.distances.Distance;
import lib.parser.aptaplex.distances.EditDistance;
import lib.parser.aptaplex.distances.Result;
import utilities.AptaLogger;
import utilities.Configuration;

/**
 * @author Jan Hoinka
 *
 *         The consumer implementation of CapRFactory. The consumer takes items
 *         from the queue, processes them and adds them to the structure database.
 */
public class CapRFactoryConsumer implements Runnable {

	/**
	 * The queue to consume from
	 */
	private BlockingQueue<Object> queue = null;

	/**
	 * Currently processed item
	 */
	private Entry<byte[], Integer> item = null;
	
	/**
	 * Used to build the aptamer with primers 
	 */
	private StringBuilder sb = new StringBuilder();
	
	
	/**
	 * Forward and reverse primers
	 */
	private String p5 = Configuration.getParameters().getString("Experiment.primer5");
	private String p3 = Configuration.getParameters().getString("Experiment.primer3");
	
	/**
	 * The progress of the CapR instances. Writable to the consumers and
	 * thread-safe
	 */
	private AtomicInteger progress = null;

	/**
	 * Switch to let the consumer know when to finish
	 */
	private Boolean isRunning = true;
	
	/**
	 * The local capr instance
	 */
	private CapR capr = new CapR();

	
	public CapRFactoryConsumer(BlockingQueue<Object> queue, AtomicInteger progress) {

		this.queue = queue;
		this.progress = progress;

	}

	@Override
	public void run() {

		// keep taking elements from the queue
		while (isRunning) {
			try {
				Object queueElement = queue.take();

				if (queueElement == Configuration.POISON_PILL) {
					AptaLogger.log(Level.CONFIG, this.getClass(), "Encountered poison pill. Exiting thread.");
					queue.put(Configuration.POISON_PILL); // notify other threads to stop
					
					// Notify GC
					capr = null;
					
					return;
				}

				// Update the progress in a thread-safe manner
				progress.incrementAndGet();

				// process queueElement
				item = (Entry<byte[], Integer>) queueElement;

				sb.append(p5).append(new String(item.getKey())).append(p3);
				
				capr.ComputeStructuralProfile(sb.toString().getBytes(), sb.length());
				
				capr.getStructuralProfile();
				
				sb = new StringBuilder();
				
				//TODO: ADD TO STRUCTURE DATABASE

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
