/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.ecm.webui.component.explorer.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

import javax.jcr.AccessDeniedException;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.ecm.webui.component.explorer.UIDocumentWorkspace;
import org.exoplatform.ecm.webui.component.explorer.UIDrivesArea;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.ecm.webui.component.explorer.UIWorkingArea;
import org.exoplatform.ecm.webui.utils.JCRExceptionManager;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.SessionProviderFactory;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkUtils;
import org.exoplatform.services.cms.link.NodeFinder;
import org.exoplatform.services.cms.taxonomy.TaxonomyService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.search.base.AbstractPageList;
import org.exoplatform.services.wcm.search.base.NodeSearchFilter;
import org.exoplatform.services.wcm.search.base.PageListFactory;
import org.exoplatform.services.wcm.search.base.QueryData;
import org.exoplatform.services.wcm.search.base.SearchDataCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.core.UIPageIterator;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Tran The Trong
 *          trongtt@gmail.com
 * Oct 2, 2006
 * 16:37:15
 *
 * Edited by : Dang Van Minh
 *             minh.dang@exoplatform.com
 * Jan 5, 2007
 */
@ComponentConfig(
    template = "app:/groovy/webui/component/explorer/search/UISearchResult.gtmpl",
    events = {
        @EventConfig(listeners = UISearchResult.ViewActionListener.class),
        @EventConfig(listeners = UISearchResult.OpenFolderActionListener.class),
        @EventConfig(listeners = UISearchResult.SortASCActionListener.class),
        @EventConfig(listeners = UISearchResult.SortDESCActionListener.class)
    }
)
public class UISearchResult extends UIContainer {

  /**
   * Logger.
   */
  private static final Log LOG  = ExoLogger.getLogger("explorer.search.UISearchResult");

  private QueryData queryData_;
  private long searchTime_ = 0;
  private boolean flag_ = false;
  private UIPageIterator uiPageIterator_;
  private static String iconType = "";
  private static String iconScore = "";
  static private int PAGE_SIZE = 10;
  private List<String> categoryPathList = new ArrayList<String>();
  private String constraintsCondition;
  private static final String EXO_RESTORE_LOCATION = "exo:restoreLocation";
  private boolean isTaxonomyNode = false;
  private String workspaceName = null;
  private String currentPath = null;
  private AbstractPageList<RowData> pageList;

  public List<String> getCategoryPathList() { return categoryPathList; }
  public void setCategoryPathList(List<String> categoryPathListItem) {
    categoryPathList = categoryPathListItem;
  }

  public String getConstraintsCondition() { return constraintsCondition; }
  public void setConstraintsCondition(String constraintsConditionItem) {
    constraintsCondition = constraintsConditionItem;
  }

  public UISearchResult() throws Exception {
    uiPageIterator_ = addChild(UIPageIterator.class, null, null);
  }

  public void setQuery(String queryStatement, String workspaceName, String language, boolean isSystemSession) {
    queryData_ = new QueryData(queryStatement, workspaceName, language, isSystemSession);
  }
  
  public long getSearchTime() { return searchTime_; }
  public void setSearchTime(long time) { this.searchTime_ = time; }

  public List getCurrentList() throws Exception {
    return uiPageIterator_.getCurrentPageData();
  }

  public DateFormat getSimpleDateFormat() {
    Locale locale = Util.getUIPortal().getAncestorOfType(UIPortalApplication.class).getLocale();
    return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, locale);
  }

  public Session getSession() throws Exception {
    return getAncestorOfType(UIJCRExplorer.class).getTargetSession();
  }

  public Date getDateCreated(Node node) throws Exception{
    if (node.hasProperty("exo:dateCreated")) {
      return node.getProperty("exo:dateCreated").getDate().getTime();
    }
    return new GregorianCalendar().getTime();
  }

  public Node getNodeByPath(String path) throws Exception {
    try {
      JCRPath nodePath = ((SessionImpl)getSession()).getLocationFactory().parseJCRPath(path);
      return (Node)getSession().getItem(nodePath.getAsString(false));
    } catch (Exception e) {
      return null;
    }
  }

  public UIPageIterator getUIPageIterator() { return uiPageIterator_; }

  public void setTaxonomyNode(boolean isTaxonomyNode, String workspaceName, String currentPath) {
    this.isTaxonomyNode = isTaxonomyNode;
    this.workspaceName = workspaceName;
    this.currentPath = currentPath;
  }

  public boolean isTaxonomyNode() { return isTaxonomyNode; }

  public Node getSymlinkNode(Node targetNode) throws Exception {
    RepositoryService repositoryService = getApplicationComponent(RepositoryService.class);
    Session session =
      SessionProviderFactory.createSessionProvider().getSession(workspaceName, repositoryService.getCurrentRepository());
    String queryStatement =
      "select * from exo:taxonomyLink where jcr:path like '" + currentPath + "/%' " +
          "and exo:uuid='"+targetNode.getUUID()+"' " +
          "and exo:workspace='"+targetNode.getSession().getWorkspace().getName()+"' order by exo:primaryType DESC";
    QueryManager queryManager = session.getWorkspace().getQueryManager();
    Query query = queryManager.createQuery(queryStatement, Query.SQL);
    if(query.execute().getNodes().getSize() > 0) return query.execute().getNodes().nextNode();
    return null;
  }

  public void updateGrid() throws Exception {
     pageList = 
      PageListFactory.createPageList(queryData_.getQueryStatement(), 
                             queryData_.getWorkSpace(),
                             queryData_.getLanguage_(), 
                             queryData_.isSystemSession(), 
                             new NodeFilter(categoryPathList), 
                             new RowDataCreator(),
                             PAGE_SIZE,
                             0);
    uiPageIterator_.setPageList(pageList);
  }

  private static class SearchComparator implements Comparator<RowData> {
    
    public static final String SORT_TYPE = "NODE_TYPE";
    public static final String SORT_SCORE = "JCR_SCORE";
    public static final String ASC = "ASC";
    public static final String DESC = "DECS";
    
    private String sortType;
    private String orderType;
    
    public void setSortType(String value) { sortType = value; }
    public void setOrderType(String value) { orderType = value; }
    
    public int compare(RowData row1, RowData row2) {
      try {
        if (SORT_TYPE.equals(sortType.trim())) {
          String s1 = row1.getJcrPrimaryType();
          String s2 = row2.getJcrPrimaryType();
          if (DESC.equals(orderType.trim())) { return s2.compareTo(s1); }
          return s1.compareTo(s2);
        } else if (SORT_SCORE.equals(sortType.trim())) {
          Long l1 = row1.getJcrScore();
          Long l2 = row2.getJcrScore();
          if (DESC.equals(orderType.trim())) { return l2.compareTo(l1); }
          return l1.compareTo(l2);
        }
      } catch (Exception e) {
        LOG.error("Cannot compare rows", e);
      }
      return 0;
    }
  }

  public String StriptHTML(String s) {
    String[] targets = {"<div>", "</div>", "<span>", "</span>"};
    for (String target : targets) {
      s = s.replace(target, "");
    }
    return s;
  }

  static  public class ViewActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      UIJCRExplorer uiExplorer = uiSearchResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      UIApplication uiApp = uiSearchResult.getAncestorOfType(UIApplication.class);
      String workspaceName = event.getRequestContext().getRequestParameter("workspaceName");
      Item item = null;
      try {
        Session session = uiExplorer.getSessionByWorkspace(workspaceName);
        // Check if the path exists
        NodeFinder nodeFinder = uiSearchResult.getApplicationComponent(NodeFinder.class);
        item = nodeFinder.getItem(session, path);
      } catch(PathNotFoundException pa) {
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } catch(ItemNotFoundException inf) {
          uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.path-not-found", null,
              ApplicationMessage.WARNING)) ;
          event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
          return ;
      } catch(AccessDeniedException ace) {
          uiApp.addMessage(new ApplicationMessage("UIDocumentInfo.msg.access-denied", null,
                  ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } catch(RepositoryException e) {
        LOG.error("Repository cannot be found");
        uiApp.addMessage(new ApplicationMessage("UITreeExplorer.msg.repository-error", null,
            ApplicationMessage.WARNING)) ;
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages()) ;
        return ;
      } catch (Exception e) {
        JCRExceptionManager.process(uiApp, e);
        return;
      }
      if (isInTrash(item))
        return;

      UIWorkingArea uiWorkingArea = uiExplorer.getChild(UIWorkingArea.class);
      UIDocumentWorkspace uiDocumentWorkspace = uiWorkingArea.getChild(UIDocumentWorkspace.class);
      if(!uiDocumentWorkspace.isRendered()) {
        uiWorkingArea.getChild(UIDrivesArea.class).setRendered(false);
        uiWorkingArea.getChild(UIDocumentWorkspace.class).setRendered(true);
      }
      uiExplorer.setSelectNode(workspaceName, path) ;

      uiExplorer.updateAjax(event) ;
    }

    private boolean isInTrash(Item item) throws RepositoryException {
      return (item instanceof Node) &&
             ((Node) item).isNodeType(EXO_RESTORE_LOCATION);
    }
  }

  static public class OpenFolderActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      UIJCRExplorer uiExplorer = uiSearchResult.getAncestorOfType(UIJCRExplorer.class);
      String path = event.getRequestContext().getRequestParameter(OBJECTID);
      String folderPath = LinkUtils.getParentPath(path);
      Node node = null;
      try {
        node = uiExplorer.getNodeByPath(folderPath, uiExplorer.getTargetSession());
      } catch(AccessDeniedException ace) {
        UIApplication uiApp = uiSearchResult.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UISearchResult.msg.access-denied", null,
            ApplicationMessage.WARNING));
        event.getRequestContext().addUIComponentToUpdateByAjax(uiApp.getUIPopupMessages());
        return;
      } catch(Exception e) {
        LOG.error("Cannot access the node at " + folderPath, e);
      }

      uiExplorer.setSelectNode(node.getSession().getWorkspace().getName(), folderPath);
      uiExplorer.updateAjax(event);
    }
  }

  static public class SortASCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource();
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      SearchComparator comparator = new SearchComparator();
      if (objectId.equals("type")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_PRIMARYTYPE);
        comparator.setSortType(SearchComparator.SORT_TYPE);
        iconType = "BlueDownArrow";
        iconScore = "";
      } else if (objectId.equals("score")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_SCORE);
        comparator.setSortType(SearchComparator.SORT_SCORE);
        iconScore = "BlueDownArrow";
        iconType = "";
      }
      comparator.setOrderType(SearchComparator.ASC);
      uiSearchResult.pageList.setComparator(comparator);
      uiSearchResult.pageList.setOrder("ASC");
      uiSearchResult.pageList.sortData();
//      Collections.sort(uiSearchResult.currentListRows_, new SearchComparator());
//      SearchResultPageList pageList = new SearchResultPageList(uiSearchResult.queryResult_,
//          uiSearchResult.currentListRows_, PAGE_SIZE, uiSearchResult.isEndOfIterator_);
//      uiSearchResult.currentAvailablePage_ = uiSearchResult.currentListNodes_.size()/PAGE_SIZE;
//      uiSearchResult.uiPageIterator_.setSearchResultPageList(pageList);
//      uiSearchResult.uiPageIterator_.setPageList(pageList);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());
    }
  }

  static public class SortDESCActionListener extends EventListener<UISearchResult> {
    public void execute(Event<UISearchResult> event) throws Exception {
      UISearchResult uiSearchResult = event.getSource() ;
      String objectId = event.getRequestContext().getRequestParameter(OBJECTID);
      SearchComparator comparator = new SearchComparator();
      if (objectId.equals("type")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_PRIMARYTYPE);
        comparator.setSortType(SearchComparator.SORT_TYPE);
        iconType = "BlueUpArrow";
        iconScore = "";
      } else if (objectId.equals("score")) {
        uiSearchResult.pageList.setSortByField(Utils.JCR_SCORE);        
        comparator.setSortType(SearchComparator.SORT_SCORE);
        iconScore = "BlueUpArrow";
        iconType = "";
      }
      comparator.setOrderType(SearchComparator.DESC);
      uiSearchResult.pageList.setComparator(comparator);
      uiSearchResult.pageList.setOrder("DESC");
      uiSearchResult.pageList.sortData();
//      Collections.sort(uiSearchResult.currentListRows_, new SearchComparator());
//      SearchResultPageList pageList = new SearchResultPageList(uiSearchResult.queryResult_,
//          uiSearchResult.currentListRows_, PAGE_SIZE, uiSearchResult.isEndOfIterator_);
//      uiSearchResult.currentAvailablePage_ = uiSearchResult.currentListNodes_.size()/PAGE_SIZE;
//      uiSearchResult.uiPageIterator_.setSearchResultPageList(pageList);
//      uiSearchResult.uiPageIterator_.setPageList(pageList);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiSearchResult.getParent());
    }
  }
  
  public static class NodeFilter implements NodeSearchFilter {

    private List<String> categoryPathList; 
    private TaxonomyService taxonomyService;
    private NodeHierarchyCreator nodeHierarchyCreator;
    private String rootTreePath;
    
    public NodeFilter(List<String> categories) {
      taxonomyService = WCMCoreUtils.getService(TaxonomyService.class);
      nodeHierarchyCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      rootTreePath = nodeHierarchyCreator.getJcrPath(BasePath.TAXONOMIES_TREE_STORAGE_PATH);
      categoryPathList = categories;
    }
    
    @Override
    public Node filterNodeToDisplay(Node node) {
      try {
        if (node != null) {
          if ((categoryPathList != null) && (categoryPathList.size() > 0)){
            for (String categoryPath : categoryPathList) {
              int index = categoryPath.indexOf("/");
              List<String> pathCategoriesList = new ArrayList<String>();
              String searchCategory = rootTreePath + "/" + categoryPath;
              List<Node> listCategories = taxonomyService.getCategories(node, categoryPath.substring(0, index));
              for (Node category : listCategories) {
                pathCategoriesList.add(category.getPath());
              }
              if (pathCategoriesList.contains(searchCategory)) 
                return node;
            }
          } else {
            return node;
          }
        }
      } catch (RepositoryException e) {}
      return null;
    }
    
  }
  
  public static class RowDataCreator implements SearchDataCreator<RowData> {

    @Override
    public RowData createData(Node node, Row row) {
      return new RowData(row);
    }
    
  }
  
  public static class RowData {
    private String jcrPath = "";
    private String repExcerpt = "";
    private long jcrScore = 0;
    private String jcrPrimaryType = "";
    
    public RowData(Row row) {
      try {
        jcrPath = row.getValue("jcr:path").getString();
      } catch (Exception e) {}
      try {
        repExcerpt = row.getValue("rep:excerpt(.)").getString();
      } catch (Exception e) {}
      try {
        jcrScore = row.getValue("jcr:score").getLong();
      } catch (Exception e) {}
      try {
        jcrPrimaryType = row.getValue("jcr:primaryType").getString();
      } catch (Exception e) {}
    }

    public String getJcrPath() {
      return jcrPath;
    }

    public void setJcrPath(String jcrPath) {
      this.jcrPath = jcrPath;
    }

    public String getRepExcerpt() {
      return repExcerpt;
    }

    public void setRepExcerpt(String repExcerpt) {
      this.repExcerpt = repExcerpt;
    }

    public long getJcrScore() {
      return jcrScore;
    }

    public void setJcrScore(long jcrScore) {
      this.jcrScore = jcrScore;
    }
    
    public String getJcrPrimaryType() {
      return jcrPrimaryType;
    }
    
    public void setJcrPrimaryType(String value) {
      jcrPrimaryType = value;
    }
    
    public int hashCode() {
      return (jcrPath == null ? 0 : jcrPath.hashCode()) + 
             (repExcerpt == null ? 0 : repExcerpt.hashCode()) + 
             (int)jcrScore + 
             (jcrPrimaryType == null ? 0 : jcrPrimaryType.hashCode());
    }
    
    public boolean equals(Object o) {
      if (o == null) return false;
      if (! (o instanceof RowData)) return false;
      RowData data = (RowData) o;
      return (jcrPath == null && data.jcrPath == null || jcrPath.equals(data.jcrPath)) &&
             (repExcerpt == null && data.repExcerpt == null || repExcerpt.equals(data.repExcerpt)) &&
             (jcrScore == data.jcrScore) &&
             (jcrPrimaryType == null && data.jcrPrimaryType == null || jcrPrimaryType.equals(data.jcrPrimaryType));
    }
  }
}
