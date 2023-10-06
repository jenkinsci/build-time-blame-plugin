package org.jenkins.ci.plugins.buildtimeblame.analysis

import org.jfree.chart.entity.EntityCollection
import org.jfree.chart.renderer.category.StackedAreaRenderer
import org.jfree.data.category.CategoryDataset

import java.awt.Shape
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator

class MapFixStackedAreaRenderer extends StackedAreaRenderer {
    /**
     * Breaks GeneralPath hotspots from StackedAreaRenderer into separate paths for
     * each polygon. This workaround lets JFreeChart build the correct image map.
     */
    @Override
    protected void addItemEntity(EntityCollection entities,
            CategoryDataset dataset, int row, int column, Shape hotspot) {
        PathIterator pi = hotspot.getPathIterator(null, 1.0)
        float[] coords = new float[6]
        GeneralPath subPath = new GeneralPath(pi.getWindingRule())

        while (!pi.isDone()) {
            int type = pi.currentSegment(coords)

            switch (type) {
                case PathIterator.SEG_MOVETO:
                    subPath.moveTo(coords[0], coords[1])
                    break
                case PathIterator.SEG_LINETO:
                    subPath.lineTo(coords[0], coords[1])
                    break
                case PathIterator.SEG_CLOSE:
                    super.addItemEntity(entities, dataset, row, column, subPath)
                    subPath = new GeneralPath()
                    break
            }

            pi.next()
        }

        if (subPath.getCurrentPoint() != null) {
            super.addItemEntity(entities, dataset, row, column, subPath)
        }
    }
}
