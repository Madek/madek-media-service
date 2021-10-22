shared_context "configuration" do
  let(:file) { File.read("spec/support/files/small.txt") }
  let(:file_size) { 132 }
  let(:part_1) { File.read("spec/support/files/small_01") }
  let(:part_2) { File.read("spec/support/files/small_02") }
  let(:md5) { Digest::MD5.hexdigest(file) }
  let(:part_1_md5) { Digest::MD5.hexdigest(part_1) }
  let(:part_2_md5) { Digest::MD5.hexdigest(part_2) }
  let(:start_request) { faraday_client_with_token.post("uploads/#{upload_id}/start") }
  let(:complete_request) do
    faraday_client_with_token.post("uploads/#{upload_id}/complete")
  end
  let(:upload_id) do
    faraday_client_with_token(json_response: true)
      .post(
        "uploads/",
        content_type: "text/plain",
        filename: "small.txt",
        md5: md5,
        media_store_id: store.id,
        size: file_size
      )
      .body
      .fetch("id")
  end
  let(:api_token) { create(:api_token, user: user, scope_write: true) }
  let(:user_token) { api_token.token_hash }
end
