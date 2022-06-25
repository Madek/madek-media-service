require "requests/shared/authentication_error.rb"
require "requests/shared/authorization_error"
require "requests/uploads/shared/configuration"

describe "Uploads", type: :request do
  include_context "configuration"

  describe "POST: uploads/:upload_id/start" do
    let(:response) { start_request }
    let(:store) { create(:media_store, :database, :with_users, users: [user].compact) }

    shared_examples "successful response" do
      let(:parsed_body) { JSON.parse(response.body) }

      it "responds with success" do
        expect(response.status).to eq(200)
      end

      it "returns upload details with 'started' state" do
        expect(without_timestamps(parsed_body)).to eq(
          {
            id: upload_id,
            md5: md5,
            state: "started",
            media_store_id: "database",
            size: file_size,
            uploader_id: user.id,
            content_type: "text/plain",
            filename: "small.txt",
            media_file_id: nil,
            error: nil,
            sha256: nil
          }.stringify_keys
        )
      end
    end

    context "with public access" do
      let(:user) { nil }
      let(:upload_id) { '00000000-0000-0000-0000-000000000000' }

      it_raises "authentication error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      include_examples "successful response"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      include_examples "successful response"
    end

    context "for user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }

      include_examples "successful response"
    end
  end
end
