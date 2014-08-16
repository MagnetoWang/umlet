package com.baselet.element.sticking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.baselet.control.SharedConstants;
import com.baselet.diagram.draw.geom.PointDouble;
import com.baselet.element.sticking.StickingPolygon.StickLine;
import com.baselet.elementnew.element.uml.relation.PointDoubleIndexed;

public class Stickables {

	private static Logger log = Logger.getLogger(Stickables.class);

	public static StickableMap getStickingPointsWhichAreConnectedToStickingPolygon(StickingPolygon oldStickingPolygon, Collection<? extends Stickable> stickables, int maxDistance) {
		log.debug("Polygon to check: " + oldStickingPolygon);
		StickableMap returnMap = new StickableMap();
		for (final Stickable stickable : stickables) {
			for (final PointDoubleIndexed p : stickable.getStickablePoints()) {
				PointDouble absolutePointPosition = getAbsolutePosition(stickable, p);
				log.debug("Check if sticks: " + absolutePointPosition);
				for (StickLine sl : oldStickingPolygon.getStickLines()) {
					if (sl.isConnected(absolutePointPosition, maxDistance)) {
						returnMap.add(stickable, p);
					}
				}
			}
		}
		return returnMap;
	}

	public static void moveStickPointsBasedOnPolygonChanges(StickingPolygon oldStickingPolygon, StickingPolygon newStickingPolygon, StickableMap stickablePointsToCheck, int maxDistance) {
		// determine which sticklines have changed and only check sticks for them
		List<StickLineChange> changedStickLines = getChangedStickLines(oldStickingPolygon, newStickingPolygon);
		// go through all stickpoints and handle the stickline-change
		for (final Stickable stickable : stickablePointsToCheck.getStickables()) {
			List<PointChange> calculatedStickingPointChanges = calculateStickingPointChanges(stickable, stickablePointsToCheck.getStickablePoints(stickable), changedStickLines, maxDistance);
			if (!calculatedStickingPointChanges.isEmpty()) {
				List<PointDoubleIndexed> updatedChangedPoints = stickable.movePoints(calculatedStickingPointChanges);
				stickablePointsToCheck.setStickablePoints(stickable, updatedChangedPoints);
			}
		}
	}

	private static List<StickLineChange> getChangedStickLines(StickingPolygon oldStickingPolygon, StickingPolygon newStickingPolygon) {
		List<StickLineChange> changedStickLines = new ArrayList<StickLineChange>();
		Iterator<StickLine> oldLineIter = oldStickingPolygon.getStickLines().iterator();
		Iterator<StickLine> newLineIter = newStickingPolygon.getStickLines().iterator();
		while (oldLineIter.hasNext()) {
			StickLine oldLine = oldLineIter.next();
			StickLine newLine = newLineIter.next();
			if (!oldLine.equals(newLine)) {
				changedStickLines.add(new StickLineChange(oldLine, newLine));
			}
		}
		return changedStickLines;
	}

	private static List<PointChange> calculateStickingPointChanges(Stickable stickable, List<PointDoubleIndexed> stickablePoints, List<StickLineChange> changedStickLines, int maxDistance) {
		List<PointChange> changedPoints = new ArrayList<PointChange>();
		for (PointDoubleIndexed stickablePoint : stickablePoints) {
			PointDouble absolutePosOfStickablePoint = getAbsolutePosition(stickable, stickablePoint);

			StickLineChange relevantStickline = getNearestStickLineChangeWhichWillChangeTheStickPoint(changedStickLines, absolutePosOfStickablePoint, maxDistance);

			if (relevantStickline != null) {
				PointDouble newPointToUse, oldPointToUse;
				// if the end of the stickline doesn't change, use the start
				if (relevantStickline.getOld().getEnd().equals(relevantStickline.getNew().getEnd())) {
					newPointToUse = relevantStickline.getNew().getStart();
					oldPointToUse = relevantStickline.getOld().getStart();
				}
				else { // otherwise use the end
					newPointToUse = relevantStickline.getNew().getEnd();
					oldPointToUse = relevantStickline.getOld().getEnd();
				}

				int diffX = newPointToUse.getX().intValue() - oldPointToUse.getX().intValue();
				int diffY = newPointToUse.getY().intValue() - oldPointToUse.getY().intValue();
				// the diff values are in current zoom, therefore normalize them (invert operation done in getAbsolutePosition())
				int diffXdefaultZoom = diffX / stickable.getGridSize() * SharedConstants.DEFAULT_GRID_SIZE;
				int diffYdefaultZoom = diffY / stickable.getGridSize() * SharedConstants.DEFAULT_GRID_SIZE;

				// only change the point if it's still connected to the new position of the stickline (otherwise you could move a sticking relation out of a class border by resizing it too much)
				// if (relevantStickline.getNew().isConnected(new PointDouble(absolutePosOfStickablePoint.getX() + diffXdefaultZoom, absolutePosOfStickablePoint.getY() + diffYdefaultZoom), 5)) {
				changedPoints.add(new PointChange(stickablePoint.getIndex(), diffXdefaultZoom, diffYdefaultZoom));
				// }
			}
		}
		return changedPoints;
	}

	private static StickLineChange getNearestStickLineChangeWhichWillChangeTheStickPoint(List<StickLineChange> changedStickLines, PointDouble absolutePositionOfStickablePoint, int maxDistance) {
		Double lowestDistance = null;
		StickLineChange changeMatchingLowestDistance = null;

		for (StickLineChange change : changedStickLines) {
			double distance = change.getOld().getDistanceToPoint(absolutePositionOfStickablePoint);
			// update best match if this distance is in range and better than the old best match
			if (distance < maxDistance && (lowestDistance == null || distance < lowestDistance)) {
				// if distance to start end end of the stickable line has changed, move the stickable point (avoids unwanted moves (eg stickablepoint in middle and resizing top or bottom -> no move necessary))
				// if ((Line.distanceBetweenTwoPoints(change.getOld().getStart(), absolutePositionOfStickablePoint) != Line.distanceBetweenTwoPoints(change.getNew().getStart(), absolutePositionOfStickablePoint)) &&
				// (Line.distanceBetweenTwoPoints(change.getOld().getEnd(), absolutePositionOfStickablePoint) != Line.distanceBetweenTwoPoints(change.getNew().getEnd(), absolutePositionOfStickablePoint))) {
				lowestDistance = distance;
				changeMatchingLowestDistance = change;
				// }
			}

		}
		return changeMatchingLowestDistance;
	}

	private static PointDouble getAbsolutePosition(Stickable stickable, PointDouble pd) {
		// the points are located relative to the upper left corner of the relation, therefore add this corner to have it located to the upper left corner of the diagram
		int x = stickable.getRectangle().getX() + pd.getX().intValue() * stickable.getGridSize() / SharedConstants.DEFAULT_GRID_SIZE;
		int y = stickable.getRectangle().getY() + pd.getY().intValue() * stickable.getGridSize() / SharedConstants.DEFAULT_GRID_SIZE;
		return new PointDouble(x, y);
	}

}
