package axon

object TreeOps {
    fun findNode(root: Node, id: String): Node? {
        if (root.id == id) return root
        for (c in root.children) findNode(c, id)?.let { return it }
        return null
    }

    fun findParent(root: Node, childId: String): Node? {
        for (c in root.children) {
            if (c.id == childId) return root
            findParent(c, childId)?.let { return it }
        }
        return null
    }

    fun contains(parent: Node, targetId: String): Boolean {
        if (parent.id == targetId) return true
        for (c in parent.children) if (contains(c, targetId)) return true
        return false
    }

    fun uniqueName(siblings: List<Node>, base: String, ignoreId: String? = null): String {
        val clean = base.trim().ifEmpty { "Untitled" }
        val used = siblings.filter { it.id != ignoreId }.map { it.name.lowercase() }.toSet()
        if (clean.lowercase() !in used) return clean
        var i = 2
        while ("$clean $i".lowercase() in used) i++
        return "$clean $i"
    }

    fun canName(parent: Node, name: String, ignoreId: String? = null): Boolean {
        val v = name.trim().lowercase()
        return parent.children.none { it.id != ignoreId && it.name.lowercase() == v }
    }

    fun moveNode(root: Node, nodeId: String, targetId: String): Boolean {
        if (nodeId == targetId) return false
        val node = findNode(root, nodeId) ?: return false
        val target = findNode(root, targetId) ?: return false
        if (!target.isFolder()) return false
        if (contains(node, targetId)) return false

        val source = findParent(root, nodeId) ?: return false
        if (source.id == target.id) return false

        if (!canName(target, node.name)) {
            node.name = uniqueName(target.children, node.name)
        }
        source.children.remove(node)
        target.children.add(node)
        target.open = true
        return true
    }

    fun walk(n: Node, block: (Node) -> Unit) {
        block(n)
        n.children.forEach { walk(it, block) }
    }

    fun matchesSearch(n: Node, query: String): Boolean {
        if (query.isEmpty()) return true
        val q = query.lowercase()
        if (n.name.lowercase().contains(q)) return true
        return n.children.any { matchesSearch(it, query) }
    }
}
