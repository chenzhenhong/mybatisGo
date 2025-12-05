package pers.sanne.my_batis_go.line.marker;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pers.sanne.my_batis_go.utils.CommonUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * (请添加描述)
 *
 * @Author czh
 * @Date 2025/12/5 周五 下午 03:06
 * @Version 1.0.0
 */
public class MybatisJumpJavaLineMarker extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (!(element instanceof XmlAttributeValue xmlAttributeValue)) {
            return;
        }

        // <select id="xxx"> ... </select>
        XmlTag parentTag = PsiTreeUtil.getParentOfType(element, XmlTag.class);
        if (parentTag == null) {
            return;
        }

        // check if tag name is MyBatis operator: select / insert / update / delete
        if (!CommonUtil.containMybatisOperator(parentTag.getName())) {
            return;
        }

        // ensure attribute is `id="something"`
        PsiElement parent = xmlAttributeValue.getParent();
        if (!(parent instanceof XmlAttribute attribute)) {
            return;
        }
        if (!"id".equals(attribute.getName())) {
            return;
        }

        // ensure XML is under /src/main/resources/mybatis
        Project project = xmlAttributeValue.getProject();
        String mybatisPath = project.getBasePath() + "/src/main/resources/mybatis";

        PsiFile containingFile = xmlAttributeValue.getContainingFile();
        VirtualFile virtualFile = containingFile.getVirtualFile();

        if (virtualFile == null || !virtualFile.getPath().contains(mybatisPath)) {
            return;
        }

        // ensure it's an XML file
        if (!(containingFile instanceof XmlFile xmlFile)) {
            return;
        }

        XmlTag rootTag = xmlFile.getRootTag();
        if (rootTag == null) {
            return;
        }

        // must have namespace="com.xxx.xxxMapper"
        String namespace = rootTag.getAttributeValue("namespace");
        if (namespace == null || namespace.isEmpty()) {
            return;
        }

        // id value
        String queryId = xmlAttributeValue.getValue();

        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }

        String targetPath = basePath + "/src/main/java/com/dongni";
        VirtualFile directory = LocalFileSystem.getInstance().findFileByPath(targetPath);
        if (directory == null || !directory.isDirectory()) {
            return;
        }

        List<PsiElement> targets = new ArrayList<>();
        GlobalSearchScope scope = new GlobalSearchScopesCore.DirectoryScope(project, directory, true);
        PsiSearchHelper instance = PsiSearchHelper.getInstance(project);
        instance.processElementsWithWord((e, offsetInElement) -> {
            if (e instanceof PsiLiteralExpression expression) {
                if (Objects.equals(expression.getValue(), namespace + "." + queryId)) {
                    targets.add(expression);
                }
            }

            return true;
        }, scope, namespace + "." + queryId, (short) 255, false);

        if (CollectionUtils.isNotEmpty(targets)) {
            NavigationGutterIconBuilder<PsiElement> builder =
                    NavigationGutterIconBuilder.create(IconLoader.getIcon("/images/mybatis.svg"))
                            .setTargetRenderer(() -> new PsiTargetPresentationRenderer<>() {
                                @Override
                                public @Nls @NotNull String getContainerText(@NotNull PsiElement element1) {
                                    PsiFile file = element1.getContainingFile();
                                    if (file != null && file.getViewProvider().getDocument() != null) {
                                        int line = file.getViewProvider().getDocument().getLineNumber(element1.getTextOffset()) + 1;
                                        return file.getName() + ":" + line;
                                    }
                                    return "未知文件";
                                }
                            })
                            .setTargets(targets)
                            .setTooltipText("Go to java file");

            result.add(builder.createLineMarkerInfo(element));
        }
    }
}
