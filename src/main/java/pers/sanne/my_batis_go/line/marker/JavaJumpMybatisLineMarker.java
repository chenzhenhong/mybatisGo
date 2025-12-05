package pers.sanne.my_batis_go.line.marker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import pers.sanne.my_batis_go.utils.CommonUtil;

import java.util.*;

/**
 * (请添加描述)
 *
 * @Author czh
 * @Date 2025/12/5 周五 下午 03:05
 * @Version 1.0.0
 */
public class JavaJumpMybatisLineMarker extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof PsiLiteralExpression literalExpression)) {
            return;
        }

        Object rawValue = literalExpression.getValue();
        if (!(rawValue instanceof String value)) {
            return;
        }

        // must be "xxx.yyy"
        if (!value.contains(".")) {
            return;
        }

        String[] parts = value.split("\\.");
        if (parts.length != 2) {
            return;
        }

        // find enclosing method call: xxx.method("mapper.id")
        PsiMethodCallExpression methodCallExpression =
                PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class);
        if (methodCallExpression == null) {
            return;
        }

        PsiMethod method = methodCallExpression.resolveMethod();
        if (method == null) {
            return;
        }

        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null) {
            return;
        }

        // skip CommonRepository subclasses
        if (!CommonUtil.isICommonRepositorySubclass(containingClass)) {
            return;
        }

        List<PsiElement> targets = new ArrayList<>();
        Project project = element.getProject();
        for (VirtualFile vFile : this.getVirtualFiles(project)) {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile instanceof XmlFile xmlFile) {
                XmlTag rootTag = xmlFile.getRootTag();
                if (rootTag != null && parts[0].equals(rootTag.getAttributeValue("namespace"))) {
                    for (XmlTag tag : rootTag.getSubTags()) {
                        if (CommonUtil.containMybatisOperator(tag.getName()) && parts[1].equals(tag.getAttributeValue("id"))) {
                            targets.add(tag);
                        }
                    }
                }
            }
        }

        if (CollectionUtils.isNotEmpty(targets)) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(IconLoader.getIcon("/images/java.svg"))
                            .setTargets(targets)
                            .setTooltipText("Go to MyBatis Mapper");

            result.add(builder.createLineMarkerInfo(element));
        }
    }

    private Collection<VirtualFile> getVirtualFiles(Project project) {
        String basePath = project.getBasePath();
        if (basePath == null) {
            return Collections.emptyList();
        }

        String targetPath = basePath + "/src/main/resources/mybatis";
        VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (directory == null || !directory.isDirectory()) {
            return Collections.emptyList();
        }

        GlobalSearchScope scope = new GlobalSearchScopesCore.DirectoryScope(project, directory, true);
        FileType xmlFileType = FileTypeManager.getInstance().getFileTypeByExtension("xml");
        return FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, xmlFileType, scope);
    }
}
