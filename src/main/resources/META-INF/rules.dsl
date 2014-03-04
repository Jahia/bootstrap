[condition][]- its path matches {pattern}=$node.path matches "{pattern}"
[condition][]The property {property} has the value "{value}" on the {node}=ChangedPropertyFact ( name == "{property}" , stringValue == "{value}" ) from {node}.properties
[condition][]The property {property} contains the value "{value}" on the {node}=ChangedPropertyFact ( name == "{property}" , stringValue contains "{value}" ) from {node}.properties
[consequence][]Compile bootstrap.css for the {module}=bootstrapCompiler.compile({module});
[consequence][]Publish subtree rooted at that {node}=bootstrapCompiler.publish({node});
