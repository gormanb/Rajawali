/**
 * Copyright 2013 Dennis Ippel
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.rajawali3d.visitors;

import org.rajawali3d.Object3D;
import org.rajawali3d.bounds.BoundingBox;
import org.rajawali3d.bounds.BoundingSphere;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.math.vector.Vector3.Axis;
import org.rajawali3d.util.Intersector;

public class RayPickingVisitor implements INodeVisitor {
	private Vector3 mRayStart;
	private Vector3 mRayEnd;
	private Vector3 mHitPoint;
	private Object3D mPickedObject;
	
	public RayPickingVisitor(Vector3 rayStart, Vector3 rayEnd) {
		mRayStart = rayStart;
		mRayEnd = rayEnd;
		mHitPoint = new Vector3();
	}
	
	public void apply(INode node) {
		if(node instanceof Object3D) {
			Object3D o = (Object3D)node;
			if(!o.isVisible() || !o.isInFrustum()) return;
			//RajLog.d("VISITING " + o.getName());
			
			if (o.getGeometry().hasBoundingSphere()) {
				BoundingSphere bsphere = o.getGeometry().getBoundingSphere();
				bsphere.calculateBounds(o.getGeometry());
				bsphere.transform(o.getModelMatrix());
				
				if(intersectsWith(bsphere)) {
					if(mPickedObject == null ||
							(mPickedObject != null && o.getPosition().z < mPickedObject.getPosition().z))
						mPickedObject = o;
				}
			} else {
				// Assume bounding box if no bounding sphere found.
				BoundingBox bbox = o.getGeometry().getBoundingBox();
				bbox.calculateBounds(o.getGeometry());
				bbox.transform(o.getModelMatrix());
				
				if(intersectsWith(bbox)) {
					if(mPickedObject == null ||
							(mPickedObject != null && o.getPosition().z < mPickedObject.getPosition().z))
						mPickedObject = o;
				}
			}
		}
	}
	
	private boolean intersectsWith(BoundingBox bbox) {
		return Intersector.intersectRayBox(bbox, mRayStart, mRayEnd, mHitPoint);
	}
	
	private boolean intersectsWith(BoundingSphere bsphere) {
		return Intersector.intersectRaySphere(mRayStart, mRayEnd, bsphere.getPosition(), bsphere.getRadius(), mHitPoint);
	}

	public Object3D getPickedObject() {
		return mPickedObject;
	}
}
