
ruby {
  init => '
    @empty_strings = [
      "-", "", " ", "None", "UNKNOWN",
      "N/A", "NA", "(empty)"
    ].freeze

    def clean_hash(event, hash, parent)
      hash.each do |key, value|
        path = "#{parent}[#{key}]"

        clean_hash(event, value, path) if value.is_a?(Hash)

        item = event.get(path)

        if item.is_a?(Hash)
          event.remove(path) if item.empty?
        elsif item.nil? || @empty_strings.include?(item)
          event.remove(path)
        end
      end
    end
  '

  code => '
    clean_hash(event, event.to_hash, "")
  '
}

