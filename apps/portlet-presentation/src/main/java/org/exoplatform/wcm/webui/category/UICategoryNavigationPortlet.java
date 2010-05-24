/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.webui.category;

import javax.portlet.PortletMode;

import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.wcm.webui.category.config.UICategoryNavigationConfig;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.core.lifecycle.UIApplicationLifecycle;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 19, 2009
 */
@ComponentConfig(
    lifecycle = UIApplicationLifecycle.class
)
public class UICategoryNavigationPortlet extends UIPortletApplication {
  
  /** The Constant MANAGEMENT_PORTLET_POPUP_WINDOW. */
  public static final String CONFIG_POPUP_WINDOW = "UICNConfigPopupWindow";

  /** The mode. */
  private PortletMode mode = PortletMode.VIEW;
  
  private String currentPath;
  
  /**
   * Instantiates a new uI category navigation portlet.
   * 
   * @throws Exception the exception
   */
  public UICategoryNavigationPortlet() throws Exception {
    activateMode(mode);
  }
  
  /**
   * Activate mode.
   * 
   * @param mode the mode
   * 
   * @throws Exception the exception
   */
  public void activateMode(PortletMode mode) throws Exception {
    getChildren().clear();
    addChild(UIPopupContainer.class, null, null);
    if (PortletMode.VIEW.equals(mode)) {
      addChild(UICategoryNavigationTree.class, null, null);
    } else if (PortletMode.EDIT.equals(mode)) {
      addChild(UICategoryNavigationConfig.class, null, null);
    } 
  }
  
  /* (non-Javadoc)
   * @see org.exoplatform.webui.core.UIPortletApplication#processRender(org.exoplatform.webui.application.WebuiApplication, org.exoplatform.webui.application.WebuiRequestContext)
   */
  public void processRender(WebuiApplication app, WebuiRequestContext context) throws Exception {
    PortletRequestContext pContext = (PortletRequestContext) context;
    if(Util.getPortalRequestContext().getRequestParameter("path") != null) {
      currentPath = Util.getPortalRequestContext().getRequestParameter("path").substring(1);
    }
    
    PortletMode newMode = pContext.getApplicationMode();
    if (!mode.equals(newMode)) {
      activateMode(newMode);
      mode = newMode;
    }
    super.processRender(app, context);
  }
  
  public String getCurrentPath() {
    return currentPath;
  }
  
  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }
}
