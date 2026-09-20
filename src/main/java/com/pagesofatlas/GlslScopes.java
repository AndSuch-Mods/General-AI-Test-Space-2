package com.pagesofatlas;

import io.github.douira.glsl_transformer.ast.node.*;
import io.github.douira.glsl_transformer.ast.node.abstract_node.ASTNode;
import io.github.douira.glsl_transformer.ast.node.declaration.*;
import io.github.douira.glsl_transformer.ast.node.expression.ReferenceExpression;
import io.github.douira.glsl_transformer.ast.node.external_declaration.FunctionDefinition;
import io.github.douira.glsl_transformer.ast.node.statement.CompoundStatement;
import io.github.douira.glsl_transformer.ast.node.statement.loop.LoopStatement;
import io.github.douira.glsl_transformer.ast.node.statement.selection.SelectionStatement;
import io.github.douira.glsl_transformer.ast.print.ASTPrinter;
import io.github.douira.glsl_transformer.ast.query.RootSupplier;
import io.github.douira.glsl_transformer.ast.transform.ASTParser;
import io.github.douira.glsl_transformer.ast.traversal.ASTBaseVisitor;
import java.util.*;

/** Resolve local bindings before sampler specialization. Uses the exact parser bundled by Iris. */
final class GlslScopes {
    private GlslScopes() {}
    private static TranslationUnit parse(String source) {
        return new ASTParser().parseTranslationUnit(RootSupplier.DEFAULT, source);
    }
    static String protectLocals(String source, Collection<String> names) {
        var tree = parse(source);
        var scopes = new ArrayDeque<Map<String, String>>();
        new ASTBaseVisitor<Void>() {
            int serial;
            String fresh() {
                String name;
                do { name = "poa_local_" + serial++; } while (source.contains(name));
                return name;
            }
            @Override public Void visit(ASTNode node) {
                if (node == null) return null;
                boolean scope = node instanceof FunctionDefinition || node instanceof CompoundStatement
                        || node instanceof LoopStatement || node instanceof SelectionStatement;
                if (scope) scopes.push(new HashMap<>());
                Identifier declared = node instanceof DeclarationMember d ? d.getName()
                        : node instanceof FunctionParameter p ? p.getName()
                        : node instanceof IterationConditionInitializer c ? c.getName() : null;
                String old = declared == null ? null : declared.getName();
                String replacement = old != null && names.contains(old) && !scopes.isEmpty() ? fresh() : null;
                if (replacement != null) declared.setName(replacement);
                if (node instanceof ReferenceExpression ref) {
                    String name = ref.getIdentifier().getName();
                    for (var bindings : scopes) if (bindings.containsKey(name)) {
                        ref.getIdentifier().setName(bindings.get(name)); break;
                    }
                }
                // GLSL local scope starts after its initializer; parameters are visible to the body.
                super.visit(node);
                if (replacement != null) scopes.peek().put(old, replacement);
                if (scope) scopes.pop();
                return null;
            }
        }.startVisit(tree);
        return ASTPrinter.printSimple(tree);
    }
    static boolean hasReference(String source, String name) {
        return parse(source).getRoot().nodeIndex.getStream(ReferenceExpression.class)
                .anyMatch(ref -> ref.getIdentifier().getName().equals(name));
    }
}
