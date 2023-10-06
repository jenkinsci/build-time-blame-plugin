package org.jenkins.ci.plugins.buildtimeblame.analysis

import org.jfree.chart.entity.EntityCollection
import org.jfree.data.category.CategoryDataset
import spock.lang.Specification

import java.awt.Shape
import java.awt.geom.GeneralPath
import java.awt.geom.PathIterator

class MapFixStackedAreaRendererTest extends Specification {
    def 'should split compound paths'() {
        given:
        def firstPath = new GeneralPath()
        firstPath.moveTo(110.0, 110.0)
        firstPath.lineTo(190.0, 110.0)
        firstPath.lineTo(190.0, 190.0)

        def secondPath = new GeneralPath()
        secondPath.moveTo(210.0, 210.0)
        secondPath.lineTo(290.0, 210.0)
        secondPath.lineTo(290.0, 290.0)

        def compoundPath = new GeneralPath()
        compoundPath.append(firstPath, false)
        compoundPath.closePath()
        compoundPath.append(secondPath, false)

        def renderer = new MapFixStackedAreaRenderer()
        def entityCollection = Mock(EntityCollection)
        def dataset = Mock(CategoryDataset) { _ * /get(Row|Column)Key/(_) >> '0' }

        when:
        renderer.addItemEntity(entityCollection, dataset, 0, 0, compoundPath)

        then:
        1 * entityCollection.add({ pathsMatch(it.getArea(), firstPath) })

        then:
        1 * entityCollection.add({ pathsMatch(it.getArea(), secondPath) })
    }

    private void pathsMatch(Shape a, Shape b) {
        PathIterator iteratorA = a.getPathIterator(null, 1.0)
        PathIterator iteratorB = b.getPathIterator(null, 1.0)

        while (!iteratorA.isDone() && !iteratorB.isDone()) {
            def segCoordsA = new float[6]
            int segTypeA = iteratorA.currentSegment(segCoordsA)

            def segCoordsB = new float[6]
            int segTypeB = iteratorB.currentSegment(segCoordsB)

            assert segTypeA == segTypeB
            assert Arrays.equals(segCoordsA, segCoordsB)

            iteratorA.next()
            iteratorB.next()
        }

        assert iteratorA.isDone()
        assert iteratorB.isDone()
    }
}
