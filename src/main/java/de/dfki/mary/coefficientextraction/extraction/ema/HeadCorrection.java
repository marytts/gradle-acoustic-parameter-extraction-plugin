package de.dfki.mary.coefficientextraction.extraction.ema;

import org.ejml.simple.SimpleMatrix;
import java.util.ArrayList;


public class HeadCorrection {

    private static final int NB_POS = 3;

    // Useful channel (T3, T2, T1, ref, jaw, upperlip, lowerlip)
    private static final int[] CHANNEL_LIST = {0, 8, 16, 24, 32, 64, 72};

    // Indexes of the reference
    private static final int FRONT_INDEX = 40; // actually named "nose" in mngu0
    private static final int LEFT_INDEX = 48;
    private static final int RIGHT_INDEX = 56;

	// Ema data structures
	private SimpleMatrix data;
	private int numberOfFieldsPerChannel;
    private	int numberOfChannels;

	// reference channel indexes
	private int frontIndex;
	private int leftIndex;
	private int rightIndex;

	// for representing the transformation

	// origin of local coordinate system
	private SimpleMatrix origin;

	/* matrix for mapping a vector into the local coordinate system
	 *
	 * given a head, the orientation of coordinate system is:
	 *
	 * x-axis: left to right
	 * y-axis: front to back
	 * z-axis: bottom to top
	 *
	 */
	private SimpleMatrix rotation;

	public HeadCorrection()
    {
		this.rotation = new SimpleMatrix(NB_POS, NB_POS);
	}


	public ArrayList<Float> performCorrection(ArrayList<Float> frame)
    {
        // compute transformation matrix
        computeTransformation(frame);

        // perform motion correction
        return applyTransformation(frame);
	}

	private void computeTransformation(ArrayList<Float> frame)
    {

		// get the reference positions
		SimpleMatrix front = getPosition(frame, FRONT_INDEX);
		SimpleMatrix left = getPosition(frame, LEFT_INDEX);
		SimpleMatrix right = getPosition(frame, RIGHT_INDEX);

		computeOrigin(front, left, right);
		computeRotation(front, left, right);

	}

	private void computeOrigin(SimpleMatrix front, SimpleMatrix left, SimpleMatrix right)
    {
		this.origin = front.plus(left.plus(right)).divide(3); // 3 = card({front, left, right})
	}

	private void computeRotation(SimpleMatrix front, SimpleMatrix left, SimpleMatrix right)
    {

		SimpleMatrix leftToRight = right.minus(left);
		SimpleMatrix frontToLeft = left.minus(front);

		SimpleMatrix xAxis = normalize(leftToRight);
		SimpleMatrix zAxis = normalize(cross(frontToLeft, xAxis));
		SimpleMatrix yAxis = normalize(cross(xAxis, zAxis));

        for (int column=0; column <NB_POS; column++)
        {
			this.rotation.set(0, column, xAxis.get(column, 0));
            this.rotation.set(1, column, yAxis.get(column, 0));
			this.rotation.set(2, column, zAxis.get(column, 0));
		}
	}

    private ArrayList<Float> applyTransformation(ArrayList<Float> frame)
    {

        ArrayList<Float> results = new ArrayList<Float>();

        for (int i=0; i<CHANNEL_LIST.length; i++)
        {
            int channelIndex = CHANNEL_LIST[i];
            SimpleMatrix position = getPosition(frame, channelIndex);
			SimpleMatrix shiftedPosition = position.minus(this.origin);
			SimpleMatrix transformedPosition = this.rotation.mult(shiftedPosition);

            for (int j=0; j<transformedPosition.numRows(); j++) // FIXME: for now ignore z !
            {
                results.add((float) transformedPosition.get(j, 0));
            }
		}

        return results;
	}

	// helper method for getting the position of the given channel at a specific time
    private SimpleMatrix getPosition(ArrayList<Float> frame, int start_index)
    {
        SimpleMatrix output = new SimpleMatrix(NB_POS, 1);
        for (int i=0;i<NB_POS;i++)
        {
            output.set(i, 0, frame.get(start_index+i));
        }
		return output;

	}

	// helper method for computing the cross product
	private SimpleMatrix cross(SimpleMatrix u, SimpleMatrix v)
    {
		SimpleMatrix  result = new SimpleMatrix(NB_POS, 1);

		// u_2 * v_3 - u_3 * v_2
		result.set(0, 0, u.get(1) * v.get(2) - u.get(2) * v.get(1) );
		// u_3 * v_1 - u_1 * v_3
		result.set(1, 0, u.get(2) * v.get(0) - u.get(0) * v.get(2) );
		// u_1 * v_2 - u_2 * v_1
		result.set(2, 0, u.get(0) * v.get(1) - u.get(1) * v.get(0) );

		return result;
	}

	// helper method for normalizing a vector
	private SimpleMatrix normalize(SimpleMatrix  u)
    {
		SimpleMatrix squaredLength = u.transpose().mult(u);
		SimpleMatrix result = u.divide(Math.sqrt(squaredLength.get(0,0)));

		return result;
	}

}
